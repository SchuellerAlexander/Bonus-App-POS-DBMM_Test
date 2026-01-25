package at.htlle.service;

import at.htlle.entity.Branch;
import at.htlle.entity.PointRule;
import at.htlle.entity.Restaurant;
import at.htlle.entity.Reward;
import at.htlle.repository.BranchRepository;
import at.htlle.repository.PointRuleRepository;
import at.htlle.repository.RestaurantRepository;
import at.htlle.repository.RewardRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdminRestaurantService {

    private static final String DEFAULT_POINT_RULE_NAME = "Default Points";

    private final RestaurantRepository restaurantRepository;
    private final BranchRepository branchRepository;
    private final RewardRepository rewardRepository;
    private final PointRuleRepository pointRuleRepository;

    public AdminRestaurantService(RestaurantRepository restaurantRepository,
                                  BranchRepository branchRepository,
                                  RewardRepository rewardRepository,
                                  PointRuleRepository pointRuleRepository) {
        this.restaurantRepository = restaurantRepository;
        this.branchRepository = branchRepository;
        this.rewardRepository = rewardRepository;
        this.pointRuleRepository = pointRuleRepository;
    }

    public List<Restaurant> listRestaurants() {
        return Optional.ofNullable(restaurantRepository.findAll())
                .orElseGet(Collections::emptyList)
                .stream()
                .filter(restaurant -> restaurant != null)
                .sorted(Comparator.comparing(Restaurant::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
    }

    public List<Branch> listBranches() {
        return Optional.ofNullable(branchRepository.findAll())
                .orElseGet(Collections::emptyList)
                .stream()
                .filter(branch -> branch != null)
                .sorted(Comparator.comparing(Branch::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
    }

    public List<Reward> listRewards() {
        return Optional.ofNullable(rewardRepository.findAll())
                .orElseGet(Collections::emptyList)
                .stream()
                .filter(reward -> reward != null)
                .sorted(Comparator.comparing(Reward::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
    }

    public List<Branch> listBranchesForRestaurant(Long restaurantId) {
        if (restaurantId == null) {
            return Collections.emptyList();
        }
        return Optional.ofNullable(branchRepository.findByRestaurantId(restaurantId))
                .orElseGet(Collections::emptyList)
                .stream()
                .filter(branch -> branch != null)
                .sorted(Comparator.comparing(Branch::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
    }

    public List<Reward> listRewardsForRestaurant(Long restaurantId) {
        if (restaurantId == null) {
            return Collections.emptyList();
        }
        return Optional.ofNullable(rewardRepository.findByRestaurantId(restaurantId))
                .orElseGet(Collections::emptyList)
                .stream()
                .filter(reward -> reward != null)
                .sorted(Comparator.comparing(Reward::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
    }

    public Optional<PointRule> findDefaultRule(Long restaurantId) {
        if (restaurantId == null) {
            return Optional.empty();
        }
        return pointRuleRepository.findByRestaurantIdAndName(restaurantId, DEFAULT_POINT_RULE_NAME);
    }

    @Transactional
    public Restaurant createRestaurant(String name, String code, boolean active, String defaultCurrency) {
        Restaurant restaurant = new Restaurant();
        restaurant.setName(normalizeText(name));
        restaurant.setCode(normalizeCode(code));
        restaurant.setActive(active);
        restaurant.setDefaultCurrency(normalizeCurrency(defaultCurrency));
        return restaurantRepository.save(restaurant);
    }

    @Transactional
    public Restaurant updateRestaurant(Long restaurantId, String name, String code, boolean active, String defaultCurrency) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));
        restaurant.setName(normalizeText(name));
        restaurant.setCode(normalizeCode(code));
        restaurant.setActive(active);
        restaurant.setDefaultCurrency(normalizeCurrency(defaultCurrency));
        return restaurantRepository.save(restaurant);
    }

    @Transactional
    public void deleteRestaurant(Long restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));
        restaurantRepository.delete(restaurant);
    }

    @Transactional
    public Branch createBranch(Long restaurantId, String branchCode, String name, boolean defaultBranch) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));
        Branch branch = new Branch();
        branch.setRestaurant(restaurant);
        branch.setBranchCode(normalizeCode(branchCode));
        branch.setName(normalizeText(name));
        branch.setDefaultBranch(defaultBranch);
        if (defaultBranch) {
            clearDefaultBranch(restaurantId);
        }
        return branchRepository.save(branch);
    }

    @Transactional
    public Branch updateBranch(Long branchId, Long restaurantId, String branchCode, String name, boolean defaultBranch) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new IllegalArgumentException("Branch not found"));
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));
        branch.setRestaurant(restaurant);
        branch.setBranchCode(normalizeCode(branchCode));
        branch.setName(normalizeText(name));
        branch.setDefaultBranch(defaultBranch);
        if (defaultBranch) {
            clearDefaultBranch(restaurantId);
        }
        return branchRepository.save(branch);
    }

    @Transactional
    public void deleteBranch(Long branchId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new IllegalArgumentException("Branch not found"));
        branchRepository.delete(branch);
    }

    @Transactional
    public PointRule setDefaultPointRule(Long restaurantId, BigDecimal pointsPerEuro, boolean active) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));
        PointRule rule = pointRuleRepository.findByRestaurantIdAndName(restaurantId, DEFAULT_POINT_RULE_NAME)
                .orElseGet(PointRule::new);
        rule.setRestaurant(restaurant);
        rule.setName(DEFAULT_POINT_RULE_NAME);
        rule.setRuleType(PointRule.RuleType.MULTIPLIER);
        rule.setMultiplier(pointsPerEuro);
        rule.setAmountThreshold(BigDecimal.ONE);
        rule.setBasePoints(0);
        rule.setActive(active);
        return pointRuleRepository.save(rule);
    }

    @Transactional
    public Reward createReward(Long restaurantId,
                               String rewardCode,
                               String name,
                               String description,
                               Integer costPoints,
                               Reward.RewardType rewardType,
                               boolean active) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));
        Reward reward = new Reward();
        reward.setRestaurant(restaurant);
        reward.setRewardCode(normalizeCode(rewardCode));
        reward.setName(normalizeText(name));
        reward.setDescription(description != null ? description.trim() : null);
        reward.setCostPoints(costPoints);
        reward.setRewardType(rewardType != null ? rewardType : Reward.RewardType.PRODUCT);
        reward.setActive(active);
        return rewardRepository.save(reward);
    }

    @Transactional
    public Reward updateReward(Long rewardId,
                               Long restaurantId,
                               String rewardCode,
                               String name,
                               String description,
                               Integer costPoints,
                               Reward.RewardType rewardType,
                               boolean active) {
        Reward reward = rewardRepository.findById(rewardId)
                .orElseThrow(() -> new IllegalArgumentException("Reward not found"));
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));
        reward.setRestaurant(restaurant);
        reward.setRewardCode(normalizeCode(rewardCode));
        reward.setName(normalizeText(name));
        reward.setDescription(description != null ? description.trim() : null);
        reward.setCostPoints(costPoints);
        reward.setRewardType(rewardType != null ? rewardType : Reward.RewardType.PRODUCT);
        reward.setActive(active);
        return rewardRepository.save(reward);
    }

    @Transactional
    public void deleteReward(Long rewardId) {
        Reward reward = rewardRepository.findById(rewardId)
                .orElseThrow(() -> new IllegalArgumentException("Reward not found"));
        rewardRepository.delete(reward);
    }

    private void clearDefaultBranch(Long restaurantId) {
        List<Branch> branches = Optional.ofNullable(branchRepository.findByRestaurantId(restaurantId))
                .orElseGet(Collections::emptyList);
        for (Branch branch : branches) {
            if (branch != null && branch.isDefaultBranch()) {
                branch.setDefaultBranch(false);
                branchRepository.save(branch);
            }
        }
    }

    private String normalizeText(String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("Value is required");
        }
        return value.trim();
    }

    private String normalizeCode(String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("Value is required");
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeCurrency(String value) {
        if (!StringUtils.hasText(value)) {
            return "EUR";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
