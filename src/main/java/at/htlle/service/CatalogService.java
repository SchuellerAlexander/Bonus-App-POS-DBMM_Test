package at.htlle.service;

import at.htlle.dto.RestaurantSummaryResponse;
import at.htlle.dto.RewardSummaryResponse;
import at.htlle.entity.Branch;
import at.htlle.entity.Restaurant;
import at.htlle.entity.Reward;
import at.htlle.repository.BranchRepository;
import at.htlle.repository.RestaurantRepository;
import at.htlle.repository.RewardRepository;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CatalogService {

    private final RestaurantRepository restaurantRepository;
    private final RewardRepository rewardRepository;
    private final BranchRepository branchRepository;

    public CatalogService(RestaurantRepository restaurantRepository,
                          RewardRepository rewardRepository,
                          BranchRepository branchRepository) {
        this.restaurantRepository = restaurantRepository;
        this.rewardRepository = rewardRepository;
        this.branchRepository = branchRepository;
    }

    public List<RestaurantSummaryResponse> listRestaurants() {
        return restaurantRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Restaurant::getName, String.CASE_INSENSITIVE_ORDER))
                .map(restaurant -> new RestaurantSummaryResponse(
                        restaurant.getId(),
                        restaurant.getName(),
                        resolveDefaultBranchId(restaurant.getId())))
                .toList();
    }

    public List<RewardSummaryResponse> listRewardsForRestaurant(Long restaurantId) {
        LocalDate today = LocalDate.now();
        return rewardRepository.findByRestaurantIdAndActiveTrue(restaurantId)
                .stream()
                .filter(reward -> isRewardActiveForDate(reward, today))
                .sorted(Comparator.comparing(Reward::getCostPoints))
                .map(reward -> new RewardSummaryResponse(
                        reward.getId(),
                        reward.getName(),
                        reward.getDescription(),
                        reward.getCostPoints()))
                .toList();
    }

    private Long resolveDefaultBranchId(Long restaurantId) {
        return branchRepository.findFirstByRestaurantIdOrderByIdAsc(restaurantId)
                .map(Branch::getId)
                .orElse(null);
    }

    private boolean isRewardActiveForDate(Reward reward, LocalDate today) {
        if (reward.getValidFrom() != null && reward.getValidFrom().isAfter(today)) {
            return false;
        }
        return reward.getValidUntil() == null || !reward.getValidUntil().isBefore(today);
    }
}
