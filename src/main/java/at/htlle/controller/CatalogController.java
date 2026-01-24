package at.htlle.controller;

import at.htlle.dto.RestaurantSummaryResponse;
import at.htlle.dto.RewardSummaryResponse;
import at.htlle.service.CatalogService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/restaurants")
    public List<RestaurantSummaryResponse> listRestaurants() {
        return catalogService.listRestaurants();
    }

    @GetMapping("/restaurants/{id}/rewards")
    public List<RewardSummaryResponse> listRewards(@PathVariable("id") Long restaurantId) {
        return catalogService.listRewardsForRestaurant(restaurantId);
    }
}
