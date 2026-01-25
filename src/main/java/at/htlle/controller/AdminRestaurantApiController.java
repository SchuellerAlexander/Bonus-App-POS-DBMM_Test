package at.htlle.controller;

import at.htlle.dto.AdminBranchRequest;
import at.htlle.dto.AdminBranchResponse;
import at.htlle.dto.AdminPointRuleRequest;
import at.htlle.dto.AdminPointRuleResponse;
import at.htlle.dto.AdminRestaurantRequest;
import at.htlle.dto.AdminRestaurantResponse;
import at.htlle.dto.AdminRewardRequest;
import at.htlle.dto.AdminRewardResponse;
import at.htlle.dto.RedemptionCodeRequest;
import at.htlle.dto.RedemptionCodeResponse;
import at.htlle.entity.Branch;
import at.htlle.entity.PointRule;
import at.htlle.entity.Restaurant;
import at.htlle.entity.Reward;
import at.htlle.entity.RewardRedemption;
import at.htlle.service.AdminRestaurantService;
import at.htlle.service.LoyaltyService;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminRestaurantApiController {

    private final AdminRestaurantService adminRestaurantService;
    private final LoyaltyService loyaltyService;

    public AdminRestaurantApiController(AdminRestaurantService adminRestaurantService,
                                        LoyaltyService loyaltyService) {
        this.adminRestaurantService = adminRestaurantService;
        this.loyaltyService = loyaltyService;
    }

    @GetMapping("/restaurants")
    public List<AdminRestaurantResponse> listRestaurants() {
        return adminRestaurantService.listRestaurants().stream()
                .map(this::toRestaurantResponse)
                .collect(Collectors.toList());
    }

    @PostMapping("/restaurants")
    public AdminRestaurantResponse createRestaurant(@RequestBody AdminRestaurantRequest request) {
        Restaurant restaurant = adminRestaurantService.createRestaurant(
                request.name(),
                request.code(),
                request.active(),
                request.defaultCurrency());
        return toRestaurantResponse(restaurant);
    }

    @PutMapping("/restaurants/{id}")
    public AdminRestaurantResponse updateRestaurant(@PathVariable("id") Long restaurantId,
                                                    @RequestBody AdminRestaurantRequest request) {
        Restaurant restaurant = adminRestaurantService.updateRestaurant(
                restaurantId,
                request.name(),
                request.code(),
                request.active(),
                request.defaultCurrency());
        return toRestaurantResponse(restaurant);
    }

    @DeleteMapping("/restaurants/{id}")
    public void deleteRestaurant(@PathVariable("id") Long restaurantId) {
        adminRestaurantService.deleteRestaurant(restaurantId);
    }

    @GetMapping("/restaurants/{id}/branches")
    public List<AdminBranchResponse> listBranches(@PathVariable("id") Long restaurantId) {
        return adminRestaurantService.listBranchesForRestaurant(restaurantId).stream()
                .map(this::toBranchResponse)
                .collect(Collectors.toList());
    }

    @PostMapping("/restaurants/{id}/branches")
    public AdminBranchResponse createBranch(@PathVariable("id") Long restaurantId,
                                            @RequestBody AdminBranchRequest request) {
        Branch branch = adminRestaurantService.createBranch(
                restaurantId,
                request.branchCode(),
                request.name(),
                request.defaultBranch());
        return toBranchResponse(branch);
    }

    @PutMapping("/branches/{id}")
    public AdminBranchResponse updateBranch(@PathVariable("id") Long branchId,
                                            @RequestBody AdminBranchRequest request) {
        Branch branch = adminRestaurantService.updateBranch(
                branchId,
                request.restaurantId(),
                request.branchCode(),
                request.name(),
                request.defaultBranch());
        return toBranchResponse(branch);
    }

    @DeleteMapping("/branches/{id}")
    public void deleteBranch(@PathVariable("id") Long branchId) {
        adminRestaurantService.deleteBranch(branchId);
    }

    @GetMapping("/restaurants/{id}/rewards")
    public List<AdminRewardResponse> listRewards(@PathVariable("id") Long restaurantId) {
        return adminRestaurantService.listRewardsForRestaurant(restaurantId).stream()
                .map(this::toRewardResponse)
                .collect(Collectors.toList());
    }

    @PostMapping("/restaurants/{id}/rewards")
    public AdminRewardResponse createReward(@PathVariable("id") Long restaurantId,
                                            @RequestBody AdminRewardRequest request) {
        Reward reward = adminRestaurantService.createReward(
                restaurantId,
                request.rewardCode(),
                request.name(),
                request.description(),
                request.costPoints(),
                request.rewardType(),
                request.active());
        return toRewardResponse(reward);
    }

    @PutMapping("/rewards/{id}")
    public AdminRewardResponse updateReward(@PathVariable("id") Long rewardId,
                                            @RequestBody AdminRewardRequest request) {
        Reward reward = adminRestaurantService.updateReward(
                rewardId,
                request.restaurantId(),
                request.rewardCode(),
                request.name(),
                request.description(),
                request.costPoints(),
                request.rewardType(),
                request.active());
        return toRewardResponse(reward);
    }

    @DeleteMapping("/rewards/{id}")
    public void deleteReward(@PathVariable("id") Long rewardId) {
        adminRestaurantService.deleteReward(rewardId);
    }

    @PostMapping("/redemptions/redeem")
    public RedemptionCodeResponse redeemRedemptionCode(@RequestBody RedemptionCodeRequest request) {
        if (request == null || request.redemptionCode() == null) {
            throw new IllegalArgumentException("Redemption code is required");
        }
        RewardRedemption redemption = loyaltyService.redeemByCode(request.redemptionCode());
        return new RedemptionCodeResponse(
                redemption.getRedemptionCode(),
                redemption.isRedeemed(),
                redemption.getRedeemedAt());
    }

    @GetMapping("/restaurants/{id}/point-rule")
    public AdminPointRuleResponse getPointRule(@PathVariable("id") Long restaurantId) {
        Optional<PointRule> rule = adminRestaurantService.findDefaultRule(restaurantId);
        return rule.map(this::toPointRuleResponse)
                .orElseGet(() -> new AdminPointRuleResponse(null, restaurantId, "Default Points", null, false));
    }

    @PutMapping("/restaurants/{id}/point-rule")
    public AdminPointRuleResponse setPointRule(@PathVariable("id") Long restaurantId,
                                               @RequestBody AdminPointRuleRequest request) {
        PointRule rule = adminRestaurantService.setDefaultPointRule(
                restaurantId,
                request.pointsPerEuro(),
                request.active());
        return toPointRuleResponse(rule);
    }

    private AdminRestaurantResponse toRestaurantResponse(Restaurant restaurant) {
        return new AdminRestaurantResponse(
                restaurant.getId(),
                restaurant.getName(),
                restaurant.getCode(),
                restaurant.getDefaultCurrency(),
                restaurant.isActive());
    }

    private AdminBranchResponse toBranchResponse(Branch branch) {
        return new AdminBranchResponse(
                branch.getId(),
                branch.getRestaurant() != null ? branch.getRestaurant().getId() : null,
                branch.getBranchCode(),
                branch.getName(),
                branch.isDefaultBranch());
    }

    private AdminRewardResponse toRewardResponse(Reward reward) {
        return new AdminRewardResponse(
                reward.getId(),
                reward.getRestaurant() != null ? reward.getRestaurant().getId() : null,
                reward.getRewardCode(),
                reward.getName(),
                reward.getDescription(),
                reward.getCostPoints(),
                reward.getRewardType(),
                reward.isActive());
    }

    private AdminPointRuleResponse toPointRuleResponse(PointRule rule) {
        return new AdminPointRuleResponse(
                rule.getId(),
                rule.getRestaurant() != null ? rule.getRestaurant().getId() : null,
                rule.getName(),
                rule.getMultiplier(),
                rule.isActive());
    }
}
