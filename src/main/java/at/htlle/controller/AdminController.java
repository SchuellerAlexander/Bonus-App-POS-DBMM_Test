package at.htlle.controller;

import at.htlle.entity.Branch;
import at.htlle.entity.PointRule;
import at.htlle.entity.Restaurant;
import at.htlle.entity.Reward;
import at.htlle.repository.BranchRepository;
import at.htlle.repository.PointRuleRepository;
import at.htlle.repository.RestaurantRepository;
import at.htlle.repository.RewardRepository;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final String DEFAULT_POINT_RULE_NAME = "Default Points";

    private final RestaurantRepository restaurantRepository;
    private final BranchRepository branchRepository;
    private final RewardRepository rewardRepository;
    private final PointRuleRepository pointRuleRepository;

    public AdminController(RestaurantRepository restaurantRepository,
                           BranchRepository branchRepository,
                           RewardRepository rewardRepository,
                           PointRuleRepository pointRuleRepository) {
        this.restaurantRepository = restaurantRepository;
        this.branchRepository = branchRepository;
        this.rewardRepository = rewardRepository;
        this.pointRuleRepository = pointRuleRepository;
    }

    @GetMapping
    public String adminHome(Model model) {
        loadAdminData(model);
        return "admin";
    }

    @PostMapping("/restaurants/create")
    public String createRestaurant(@RequestParam("name") String name,
                                   @RequestParam("code") String code,
                                   @RequestParam(name = "active", defaultValue = "false") boolean active,
                                   @RequestParam(name = "defaultCurrency", defaultValue = "EUR") String defaultCurrency) {
        Restaurant restaurant = new Restaurant();
        restaurant.setName(name.trim());
        restaurant.setCode(code.trim().toUpperCase(Locale.ROOT));
        restaurant.setActive(active);
        restaurant.setDefaultCurrency(defaultCurrency.trim().toUpperCase(Locale.ROOT));
        restaurantRepository.save(restaurant);
        return "redirect:/admin";
    }

    @PostMapping("/restaurants/{id}/update")
    public String updateRestaurant(@PathVariable("id") Long restaurantId,
                                   @RequestParam("name") String name,
                                   @RequestParam("code") String code,
                                   @RequestParam(name = "active", defaultValue = "false") boolean active,
                                   @RequestParam(name = "defaultCurrency", defaultValue = "EUR") String defaultCurrency) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));
        restaurant.setName(name.trim());
        restaurant.setCode(code.trim().toUpperCase(Locale.ROOT));
        restaurant.setActive(active);
        restaurant.setDefaultCurrency(defaultCurrency.trim().toUpperCase(Locale.ROOT));
        restaurantRepository.save(restaurant);
        return "redirect:/admin";
    }

    @PostMapping("/points-rules/set")
    public String setPointsRule(@RequestParam("restaurantId") Long restaurantId,
                                @RequestParam("pointsPerEuro") BigDecimal pointsPerEuro,
                                @RequestParam(name = "active", defaultValue = "true") boolean active) {
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
        pointRuleRepository.save(rule);
        return "redirect:/admin";
    }

    @PostMapping("/branches/create")
    public String createBranch(@RequestParam("restaurantId") Long restaurantId,
                               @RequestParam("branchCode") String branchCode,
                               @RequestParam("name") String name,
                               @RequestParam(name = "defaultBranch", defaultValue = "false") boolean defaultBranch) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));
        Branch branch = new Branch();
        branch.setRestaurant(restaurant);
        branch.setBranchCode(branchCode.trim().toUpperCase(Locale.ROOT));
        branch.setName(name.trim());
        branch.setDefaultBranch(defaultBranch);
        if (defaultBranch) {
            clearDefaultBranch(restaurantId);
        }
        branchRepository.save(branch);
        return "redirect:/admin";
    }

    @PostMapping("/branches/{id}/update")
    public String updateBranch(@PathVariable("id") Long branchId,
                               @RequestParam("restaurantId") Long restaurantId,
                               @RequestParam("branchCode") String branchCode,
                               @RequestParam("name") String name,
                               @RequestParam(name = "defaultBranch", defaultValue = "false") boolean defaultBranch) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new IllegalArgumentException("Branch not found"));
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));
        branch.setRestaurant(restaurant);
        branch.setBranchCode(branchCode.trim().toUpperCase(Locale.ROOT));
        branch.setName(name.trim());
        branch.setDefaultBranch(defaultBranch);
        if (defaultBranch) {
            clearDefaultBranch(restaurantId);
        }
        branchRepository.save(branch);
        return "redirect:/admin";
    }

    @PostMapping("/branches/{id}/default")
    public String setDefaultBranch(@PathVariable("id") Long branchId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new IllegalArgumentException("Branch not found"));
        clearDefaultBranch(branch.getRestaurant().getId());
        branch.setDefaultBranch(true);
        branchRepository.save(branch);
        return "redirect:/admin";
    }

    @PostMapping("/rewards/create")
    public String createReward(@RequestParam("restaurantId") Long restaurantId,
                               @RequestParam("rewardCode") String rewardCode,
                               @RequestParam("name") String name,
                               @RequestParam("description") String description,
                               @RequestParam("costPoints") Integer costPoints,
                               @RequestParam(name = "active", defaultValue = "false") boolean active) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));
        Reward reward = new Reward();
        reward.setRestaurant(restaurant);
        reward.setRewardCode(rewardCode.trim().toUpperCase(Locale.ROOT));
        reward.setName(name.trim());
        reward.setDescription(description.trim());
        reward.setCostPoints(costPoints);
        reward.setActive(active);
        rewardRepository.save(reward);
        return "redirect:/admin";
    }

    @PostMapping("/rewards/{id}/update")
    public String updateReward(@PathVariable("id") Long rewardId,
                               @RequestParam("restaurantId") Long restaurantId,
                               @RequestParam("rewardCode") String rewardCode,
                               @RequestParam("name") String name,
                               @RequestParam("description") String description,
                               @RequestParam("costPoints") Integer costPoints,
                               @RequestParam(name = "active", defaultValue = "false") boolean active) {
        Reward reward = rewardRepository.findById(rewardId)
                .orElseThrow(() -> new IllegalArgumentException("Reward not found"));
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));
        reward.setRestaurant(restaurant);
        reward.setRewardCode(rewardCode.trim().toUpperCase(Locale.ROOT));
        reward.setName(name.trim());
        reward.setDescription(description.trim());
        reward.setCostPoints(costPoints);
        reward.setActive(active);
        rewardRepository.save(reward);
        return "redirect:/admin";
    }

    private void clearDefaultBranch(Long restaurantId) {
        List<Branch> branches = branchRepository.findByRestaurantId(restaurantId);
        for (Branch branch : branches) {
            if (branch.isDefaultBranch()) {
                branch.setDefaultBranch(false);
                branchRepository.save(branch);
            }
        }
    }

    private void loadAdminData(Model model) {
        List<Restaurant> restaurants = restaurantRepository.findAll().stream()
                .sorted(Comparator.comparing(Restaurant::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
        List<Branch> branches = branchRepository.findAll().stream()
                .sorted(Comparator.comparing(Branch::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
        List<Reward> rewards = rewardRepository.findAll().stream()
                .sorted(Comparator.comparing(Reward::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();

        Map<Long, PointRule> defaultRules = restaurants.stream()
                .collect(Collectors.toMap(Restaurant::getId, restaurant -> pointRuleRepository
                        .findByRestaurantIdAndName(restaurant.getId(), DEFAULT_POINT_RULE_NAME)
                        .orElse(null)));

        model.addAttribute("restaurants", restaurants);
        model.addAttribute("branches", branches);
        model.addAttribute("rewards", rewards);
        model.addAttribute("defaultRules", defaultRules);
    }
}
