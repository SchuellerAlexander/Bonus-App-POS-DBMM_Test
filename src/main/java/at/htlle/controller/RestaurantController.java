package at.htlle.controller;

import at.htlle.dto.RestaurantSummary;
import at.htlle.dto.RewardSummary;
import at.htlle.entity.Restaurant;
import at.htlle.repository.RestaurantRepository;
import at.htlle.repository.RewardRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/restaurants")
public class RestaurantController {

    private final RestaurantRepository restaurantRepository;
    private final RewardRepository rewardRepository;

    public RestaurantController(RestaurantRepository restaurantRepository, RewardRepository rewardRepository) {
        this.restaurantRepository = restaurantRepository;
        this.rewardRepository = rewardRepository;
    }

    @GetMapping
    public List<RestaurantSummary> listRestaurants() {
        return restaurantRepository.findByActiveTrue().stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}/rewards")
    public List<RewardSummary> listActiveRewards(@PathVariable("id") Long restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new EntityNotFoundException("Restaurant not found"));
        return rewardRepository.findByRestaurantIdAndActiveTrue(restaurant.getId()).stream()
                .map(reward -> new RewardSummary(
                        reward.getId(),
                        reward.getName(),
                        reward.getDescription(),
                        reward.getCostPoints()))
                .collect(Collectors.toList());
    }

    private RestaurantSummary toSummary(Restaurant restaurant) {
        return new RestaurantSummary(
                restaurant.getId(),
                restaurant.getName(),
                restaurant.getCode(),
                restaurant.getDefaultCurrency());
    }
}
