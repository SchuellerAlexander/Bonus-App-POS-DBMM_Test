package at.htlle.service;

import at.htlle.dto.PurchaseRequest;
import at.htlle.dto.RedemptionRequest;
import at.htlle.entity.LoyaltyAccount;
import at.htlle.entity.PointLedger;
import at.htlle.entity.PointRule;
import at.htlle.entity.Purchase;
import at.htlle.entity.Redemption;
import at.htlle.entity.Reward;
import at.htlle.entity.Restaurant;
import at.htlle.repository.LoyaltyAccountRepository;
import at.htlle.repository.PointLedgerRepository;
import at.htlle.repository.PointRuleRepository;
import at.htlle.repository.PurchaseRepository;
import at.htlle.repository.RedemptionRepository;
import at.htlle.repository.RestaurantRepository;
import at.htlle.repository.RewardRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.security.SecureRandom;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import org.springframework.transaction.annotation.Transactional;

@Service
public class LoyaltyService {

    private static final String REDEMPTION_CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int REDEMPTION_CODE_MIN_LENGTH = 8;
    private static final int REDEMPTION_CODE_MAX_LENGTH = 12;
    private static final int REDEMPTION_CODE_MAX_ATTEMPTS = 20;

    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final PurchaseRepository purchaseRepository;
    private final PointLedgerRepository pointLedgerRepository;
    private final PointRuleRepository pointRuleRepository;
    private final RewardRepository rewardRepository;
    private final RestaurantRepository restaurantRepository;
    private final RedemptionRepository redemptionRepository;
    private final PointCalculator pointCalculator;
    private final SecureRandom secureRandom = new SecureRandom();

    public LoyaltyService(
            LoyaltyAccountRepository loyaltyAccountRepository,
            PurchaseRepository purchaseRepository,
            PointLedgerRepository pointLedgerRepository,
            PointRuleRepository pointRuleRepository,
            RewardRepository rewardRepository,
            RestaurantRepository restaurantRepository,
            RedemptionRepository redemptionRepository,
            PointCalculator pointCalculator) {
        this.loyaltyAccountRepository = loyaltyAccountRepository;
        this.purchaseRepository = purchaseRepository;
        this.pointLedgerRepository = pointLedgerRepository;
        this.pointRuleRepository = pointRuleRepository;
        this.rewardRepository = rewardRepository;
        this.restaurantRepository = restaurantRepository;
        this.redemptionRepository = redemptionRepository;
        this.pointCalculator = pointCalculator;
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
    public Redemption redeemReward(RedemptionRequest request) {
        LoyaltyAccount account = loyaltyAccountRepository
                .lockById(request.accountId())
                .orElseThrow(() -> new EntityNotFoundException("Account not found"));

        Reward reward = rewardRepository
                .findById(request.rewardId())
                .orElseThrow(() -> new EntityNotFoundException("Reward not found"));

        Restaurant restaurant = restaurantRepository
                .findById(request.restaurantId())
                .orElseThrow(() -> new EntityNotFoundException("Restaurant not found"));

        if (!reward.isActive()) {
            throw new IllegalStateException("Reward inactive");
        }
        if (!reward.getRestaurant().getId().equals(restaurant.getId())) {
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
        redemption.setRestaurant(restaurant);
        redemption.setLedgerEntry(persistedLedger);
        redemption.setStatus(Redemption.Status.COMPLETED);
        redemption.setRedeemedAt(Instant.now());
        redemption.setPointsSpent(cost);
        redemption.setNotes(request.notes());
        redemption.setRedemptionCode(generateUniqueRedemptionCode());

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

    private String generateUniqueRedemptionCode() {
        for (int attempt = 0; attempt < REDEMPTION_CODE_MAX_ATTEMPTS; attempt++) {
            String code = generateRedemptionCode();
            if (!redemptionRepository.existsByRedemptionCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Failed to generate unique redemption code");
    }

    private String generateRedemptionCode() {
        int length = REDEMPTION_CODE_MIN_LENGTH
                + secureRandom.nextInt(REDEMPTION_CODE_MAX_LENGTH - REDEMPTION_CODE_MIN_LENGTH + 1);
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = secureRandom.nextInt(REDEMPTION_CODE_CHARS.length());
            builder.append(REDEMPTION_CODE_CHARS.charAt(index));
        }
        return builder.toString();
    }
}
