package at.htlle.service;

import at.htlle.dto.PurchaseRequest;
import at.htlle.dto.RedemptionRequest;
import at.htlle.entity.LoyaltyAccount;
import at.htlle.entity.PointLedger;
import at.htlle.entity.PointRule;
import at.htlle.entity.Purchase;
import at.htlle.entity.Reward;
import at.htlle.entity.RewardRedemption;
import at.htlle.entity.Restaurant;
import at.htlle.repository.LoyaltyAccountRepository;
import at.htlle.repository.PointLedgerRepository;
import at.htlle.repository.PointRuleRepository;
import at.htlle.repository.PurchaseRepository;
import at.htlle.repository.RestaurantRepository;
import at.htlle.repository.RewardRepository;
import at.htlle.repository.RewardRedemptionRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import jakarta.persistence.EntityNotFoundException;
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
    private final RestaurantRepository restaurantRepository;
    private final RewardRedemptionRepository rewardRedemptionRepository;
    private final PointCalculator pointCalculator;
    private final RewardRedemptionCodeGenerator rewardRedemptionCodeGenerator;

    public LoyaltyService(
            LoyaltyAccountRepository loyaltyAccountRepository,
            PurchaseRepository purchaseRepository,
            PointLedgerRepository pointLedgerRepository,
            PointRuleRepository pointRuleRepository,
            RewardRepository rewardRepository,
            RestaurantRepository restaurantRepository,
            RewardRedemptionRepository rewardRedemptionRepository,
            PointCalculator pointCalculator,
            RewardRedemptionCodeGenerator rewardRedemptionCodeGenerator) {
        this.loyaltyAccountRepository = loyaltyAccountRepository;
        this.purchaseRepository = purchaseRepository;
        this.pointLedgerRepository = pointLedgerRepository;
        this.pointRuleRepository = pointRuleRepository;
        this.rewardRepository = rewardRepository;
        this.restaurantRepository = restaurantRepository;
        this.rewardRedemptionRepository = rewardRedemptionRepository;
        this.pointCalculator = pointCalculator;
        this.rewardRedemptionCodeGenerator = rewardRedemptionCodeGenerator;
    }

    @Transactional
    public PointLedger recordPurchase(PurchaseRequest request) {
        LoyaltyAccount account = loyaltyAccountRepository
                .lockById(request.accountId())
                .orElseThrow(() -> new EntityNotFoundException("Account not found"));

        if (purchaseRepository.findByPurchaseNumber(request.purchaseNumber()).isPresent()) {
            throw new IllegalArgumentException("Purchase number already exists");
        }
        if (request.totalAmount() == null || request.totalAmount().signum() <= 0) {
            throw new IllegalArgumentException("Total amount must be greater than zero");
        }

        Purchase purchase = new Purchase();
        purchase.setLoyaltyAccount(account);
        purchase.setPurchaseNumber(request.purchaseNumber());
        purchase.setCurrency(request.currency().trim().toUpperCase(Locale.ROOT));
        purchase.setTotalAmount(request.totalAmount());
        purchase.setPurchasedAt(Optional.ofNullable(request.purchasedAt()).orElse(Instant.now()));
        purchase.setNotes(request.notes());

        Restaurant restaurant = restaurantRepository
                .findById(request.restaurantId())
                .orElseThrow(() -> new EntityNotFoundException("Restaurant not found"));
        purchase.setRestaurant(restaurant);

        Purchase persisted = purchaseRepository.save(purchase);

        PointRule appliedRule;
        if (request.pointRuleId() != null) {
            appliedRule = pointRuleRepository
                    .findById(request.pointRuleId())
                    .orElseThrow(() -> new EntityNotFoundException("Point rule not found"));
            if (!appliedRule.getRestaurant().getId().equals(restaurant.getId())) {
                throw new IllegalArgumentException("Point rule does not belong to restaurant");
            }
            if (!pointCalculator.isRuleActive(appliedRule, persisted.getPurchasedAt())) {
                throw new IllegalStateException("Point rule is not active");
            }
        } else {
            List<PointRule> candidates = pointRuleRepository.findActiveRulesForDate(
                    restaurant.getId(),
                    LocalDate.ofInstant(persisted.getPurchasedAt(), java.time.ZoneId.systemDefault()));
            appliedRule = candidates.stream()
                    .sorted(Comparator
                            .comparing(PointRule::getValidFrom, Comparator.nullsLast(Comparator.naturalOrder()))
                            .reversed()
                            .thenComparing(PointRule::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No active point rule found"));
        }

        long points = pointCalculator.calculatePoints(persisted.getTotalAmount(), appliedRule);
        if (points == 0) {
            throw new IllegalStateException("Calculated points is zero");
        }
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

    @Transactional
    public RewardRedemption redeemReward(RedemptionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Redemption request is required");
        }
        return redeemReward(request.accountId(), request.rewardId(), request.notes());
    }

    @Transactional
    public RewardRedemption redeemReward(Long accountId, Long rewardId, String notes) {
        LoyaltyAccount account = loyaltyAccountRepository
                .lockById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found"));

        Reward reward = rewardRepository
                .findById(rewardId)
                .orElseThrow(() -> new EntityNotFoundException("Reward not found"));

        if (!reward.isActive()) {
            throw new IllegalStateException("Reward inactive");
        }
        Restaurant restaurant = reward.getRestaurant();
        if (restaurant == null || restaurant.getId() == null) {
            throw new IllegalStateException("Reward is not linked to a restaurant");
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
        ledger.setDescription(notes != null ? notes : "Reward redemption");
        ledger.setOccurredAt(Instant.now());

        account.setCurrentPoints(newBalance);
        loyaltyAccountRepository.save(account);
        pointLedgerRepository.save(ledger);

        RewardRedemption redemption = new RewardRedemption();
        redemption.setLoyaltyAccount(account);
        redemption.setReward(reward);
        redemption.setRedemptionCode(rewardRedemptionCodeGenerator.generateUniqueCode());
        redemption.setRedeemed(false);
        redemption.setRedeemedAt(null);
        return rewardRedemptionRepository.save(redemption);
    }

    @Transactional
    public RewardRedemption redeemByCode(String redemptionCode) {
        if (!StringUtils.hasText(redemptionCode)) {
            throw new IllegalArgumentException("Redemption code is required");
        }
        RewardRedemption redemption = rewardRedemptionRepository.findByRedemptionCode(redemptionCode.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new IllegalArgumentException("Redemption code not found"));
        if (redemption.isRedeemed()) {
            throw new IllegalStateException("Redemption code already used");
        }
        redemption.setRedeemed(true);
        redemption.setRedeemedAt(Instant.now());
        return rewardRedemptionRepository.save(redemption);
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
