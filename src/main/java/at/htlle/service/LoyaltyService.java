package at.htlle.service;

import at.htlle.dto.PurchaseRequest;
import at.htlle.dto.RedemptionRequest;
import at.htlle.entity.Branch;
import at.htlle.entity.LoyaltyAccount;
import at.htlle.entity.PointLedger;
import at.htlle.entity.PointRule;
import at.htlle.entity.Purchase;
import at.htlle.entity.Redemption;
import at.htlle.entity.Reward;
import at.htlle.repository.BranchRepository;
import at.htlle.repository.LoyaltyAccountRepository;
import at.htlle.repository.PointLedgerRepository;
import at.htlle.repository.PointRuleRepository;
import at.htlle.repository.PurchaseRepository;
import at.htlle.repository.RedemptionRepository;
import at.htlle.repository.RewardRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import org.springframework.transaction.annotation.Transactional;

@Service
public class LoyaltyService {

    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final PurchaseRepository purchaseRepository;
    private final PointLedgerRepository pointLedgerRepository;
    private final PointRuleRepository pointRuleRepository;
    private final RewardRepository rewardRepository;
    private final BranchRepository branchRepository;
    private final RedemptionRepository redemptionRepository;

    public LoyaltyService(
            LoyaltyAccountRepository loyaltyAccountRepository,
            PurchaseRepository purchaseRepository,
            PointLedgerRepository pointLedgerRepository,
            PointRuleRepository pointRuleRepository,
            RewardRepository rewardRepository,
            BranchRepository branchRepository,
            RedemptionRepository redemptionRepository) {
        this.loyaltyAccountRepository = loyaltyAccountRepository;
        this.purchaseRepository = purchaseRepository;
        this.pointLedgerRepository = pointLedgerRepository;
        this.pointRuleRepository = pointRuleRepository;
        this.rewardRepository = rewardRepository;
        this.branchRepository = branchRepository;
        this.redemptionRepository = redemptionRepository;
    }

    @Transactional
    public PointLedger recordPurchase(PurchaseRequest request) {
        LoyaltyAccount account = loyaltyAccountRepository
                .lockById(request.accountId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown account"));

        Purchase purchase = new Purchase();
        purchase.setLoyaltyAccount(account);
        purchase.setPurchaseNumber(request.purchaseNumber());
        purchase.setCurrency(request.currency());
        purchase.setTotalAmount(request.totalAmount());
        purchase.setPurchasedAt(Optional.ofNullable(request.purchasedAt()).orElse(Instant.now()));
        purchase.setNotes(request.notes());

        if (request.branchId() != null) {
            Branch branch = branchRepository
                    .findById(request.branchId())
                    .orElseThrow(() -> new IllegalArgumentException("Branch not found"));
            if (!branch.getRestaurant().getId().equals(account.getRestaurant().getId())) {
                throw new IllegalArgumentException("Branch does not belong to restaurant");
            }
            purchase.setBranch(branch);
        }

        Purchase persisted = purchaseRepository.save(purchase);

        PointRule appliedRule = null;
        if (request.pointRuleId() != null) {
            appliedRule = pointRuleRepository
                    .findById(request.pointRuleId())
                    .filter(rule -> isRuleActive(rule, persisted.getPurchasedAt()))
                    .orElseThrow(() -> new IllegalArgumentException("Point rule is not active"));
            if (!appliedRule.getRestaurant().getId().equals(account.getRestaurant().getId())) {
                throw new IllegalArgumentException("Point rule does not belong to restaurant");
            }
        }

        long points = calculatePoints(persisted.getTotalAmount(), appliedRule);
        long newBalance = account.getCurrentPoints() + points;

        PointLedger ledger = new PointLedger();
        ledger.setLoyaltyAccount(account);
        ledger.setEntryType(PointLedger.EntryType.EARN);
        ledger.setPoints(points);
        ledger.setBalanceAfter(newBalance);
        ledger.setOccurredAt(persisted.getPurchasedAt());
        ledger.setDescription(StringUtils.hasText(request.description()) ? request.description() : "Purchase points");
        ledger.setPurchase(persisted);
        ledger.setPointRule(appliedRule);

        account.setCurrentPoints(newBalance);

        loyaltyAccountRepository.save(account);
        return pointLedgerRepository.save(ledger);
    }

    private boolean isRuleActive(PointRule rule, Instant purchasedAt) {
        if (!rule.isActive()) {
            return false;
        }
        LocalDate reference = purchasedAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        if (rule.getValidFrom() != null && rule.getValidFrom().isAfter(reference)) {
            return false;
        }
        if (rule.getValidUntil() != null && rule.getValidUntil().isBefore(reference)) {
            return false;
        }
        return true;
    }

    private long calculatePoints(BigDecimal amount, PointRule rule) {
        if (rule == null) {
            return amount.setScale(0, RoundingMode.DOWN).longValue();
        }
        BigDecimal effectiveAmount = amount;
        if (rule.getAmountThreshold().compareTo(BigDecimal.ZERO) > 0) {
            effectiveAmount = amount.divide(rule.getAmountThreshold(), 2, RoundingMode.DOWN);
        }
        BigDecimal base = switch (rule.getRuleType()) {
            case MULTIPLIER -> effectiveAmount.multiply(rule.getMultiplier());
            case FIXED -> BigDecimal.valueOf(rule.getBasePoints());
        };
        return base.setScale(0, RoundingMode.DOWN).longValue();
    }

    @Transactional
    public Redemption redeemReward(RedemptionRequest request) {
        LoyaltyAccount account = loyaltyAccountRepository
                .lockById(request.accountId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown account"));

        Reward reward = rewardRepository
                .findById(request.rewardId())
                .orElseThrow(() -> new IllegalArgumentException("Reward not found"));

        if (!reward.isActive()) {
            throw new IllegalStateException("Reward inactive");
        }
        if (!reward.getRestaurant().getId().equals(account.getRestaurant().getId())) {
            throw new IllegalArgumentException("Reward does not belong to restaurant");
        }
        LocalDate today = LocalDate.now();
        if (reward.getValidFrom() != null && reward.getValidFrom().isAfter(today)) {
            throw new IllegalStateException("Reward not yet valid");
        }
        if (reward.getValidUntil() != null && reward.getValidUntil().isBefore(today)) {
            throw new IllegalStateException("Reward expired");
        }

        long cost = reward.getCostPoints();
        if (account.getCurrentPoints() < cost) {
            throw new IllegalStateException("Insufficient points");
        }

        long newBalance = account.getCurrentPoints() - cost;
        PointLedger ledger = new PointLedger();
        ledger.setLoyaltyAccount(account);
        ledger.setEntryType(PointLedger.EntryType.REDEEM);
        ledger.setPoints(-cost);
        ledger.setBalanceAfter(newBalance);
        ledger.setDescription(request.notes() != null ? request.notes() : "Reward redemption");
        ledger.setOccurredAt(Instant.now());

        account.setCurrentPoints(newBalance);
        loyaltyAccountRepository.save(account);
        PointLedger persistedLedger = pointLedgerRepository.save(ledger);

        Redemption redemption = new Redemption();
        redemption.setLoyaltyAccount(account);
        redemption.setReward(reward);
        redemption.setLedgerEntry(persistedLedger);
        redemption.setStatus(Redemption.Status.COMPLETED);
        redemption.setRedeemedAt(Instant.now());
        redemption.setPointsSpent(cost);
        redemption.setNotes(request.notes());

        Redemption saved = redemptionRepository.save(redemption);
        persistedLedger.setRedemption(saved);
        pointLedgerRepository.save(persistedLedger);
        return saved;
    }

    @Transactional
    public LoyaltyAccount synchronizeBalance(Long accountId) {
        LoyaltyAccount account = loyaltyAccountRepository
                .lockById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown account"));
        long sum = pointLedgerRepository.sumPointsForAccount(account.getId());
        account.setCurrentPoints(sum);
        return loyaltyAccountRepository.save(account);
    }
}
