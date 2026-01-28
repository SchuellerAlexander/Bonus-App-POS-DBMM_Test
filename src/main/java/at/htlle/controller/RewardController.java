package at.htlle.controller;

import at.htlle.dto.ErrorResponse;
import at.htlle.dto.RedemptionRequest;
import at.htlle.dto.RedemptionResponse;
import at.htlle.dto.RestaurantSummary;
import at.htlle.dto.RewardSummary;
import at.htlle.entity.Redemption;
import at.htlle.entity.Restaurant;
import at.htlle.entity.Reward;
import at.htlle.repository.RestaurantRepository;
import at.htlle.repository.RewardRepository;
import at.htlle.service.AccountQueryService;
import at.htlle.service.LoyaltyService;
import at.htlle.util.SessionAccountResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.HttpStatus;

@Controller
public class RewardController {

    private final LoyaltyService loyaltyService;
    private final AccountQueryService accountQueryService;
    private final RestaurantRepository restaurantRepository;
    private final RewardRepository rewardRepository;
    private final SessionAccountResolver sessionAccountResolver;

    public RewardController(LoyaltyService loyaltyService,
                            AccountQueryService accountQueryService,
                            RestaurantRepository restaurantRepository,
                            RewardRepository rewardRepository,
                            SessionAccountResolver sessionAccountResolver) {
        this.loyaltyService = loyaltyService;
        this.accountQueryService = accountQueryService;
        this.restaurantRepository = restaurantRepository;
        this.rewardRepository = rewardRepository;
        this.sessionAccountResolver = sessionAccountResolver;
    }

    @GetMapping("/rewards")
    public String rewards(@RequestParam(name = "restaurantId", required = false) Long restaurantId,
                          Model model,
                          HttpServletRequest request) {
        Long accountId = sessionAccountResolver.getAccountId(request);
        if (accountId == null) {
            return "redirect:/login";
        }
        loadRewardsPage(accountId, restaurantId, model, request, null, null);
        return "rewards";
    }

    @PostMapping("/rewards/redeem")
    public String redeem(@RequestParam("rewardId") Long rewardId,
                         @RequestParam("restaurantId") Long restaurantId,
                         @RequestParam(name = "notes", required = false) String notes,
                         Model model,
                         HttpServletRequest request) {
        Long accountId = sessionAccountResolver.getAccountId(request);
        if (accountId == null) {
            return "redirect:/login";
        }
        RedemptionRequest payload = new RedemptionRequest(accountId, rewardId, restaurantId, notes);
        try {
            Redemption redemption = loyaltyService.redeemReward(payload);
            RedemptionResponse response = new RedemptionResponse(
                    redemption.getId(),
                    redemption.getLoyaltyAccount().getId(),
                    redemption.getReward().getId(),
                    redemption.getRestaurant().getId(),
                    redemption.getLedgerEntry().getId(),
                    redemption.getPointsSpent(),
                    redemption.getLedgerEntry().getBalanceAfter(),
                    redemption.getStatus(),
                    redemption.getRedeemedAt());
            loadRewardsPage(accountId, restaurantId, model, request, null, response);
            return "rewards";
        } catch (RuntimeException ex) {
            ErrorResponse errorResponse = errorFromException(ex, request, "Failed to redeem reward");
            loadRewardsPage(accountId, restaurantId, model, request, errorResponse, null);
            return "rewards";
        }
    }

    private void loadRewardsPage(Long accountId,
                                 Long restaurantId,
                                 Model model,
                                 HttpServletRequest request,
                                 ErrorResponse errorResponse,
                                 RedemptionResponse redemptionResponse) {
        try {
            var account = accountQueryService.getAccountResponse(accountId, false);
            model.addAttribute("account", account);
            if (restaurantId == null && account != null) {
                restaurantId = account.restaurantId();
            }
        } catch (RuntimeException ex) {
            model.addAttribute("apiError", errorFromException(ex, request, "Failed to load account"));
        }
        model.addAttribute("accountId", accountId);

        List<RestaurantSummary> restaurants = fetchRestaurants();
        model.addAttribute("restaurants", restaurants);
        if (restaurantId == null && restaurants.size() == 1) {
            restaurantId = restaurants.get(0).id();
        }
        model.addAttribute("selectedRestaurantId", restaurantId);

        List<RewardSummary> rewards = List.of();
        if (restaurantId != null) {
            rewards = fetchRewards(restaurantId);
        }
        model.addAttribute("rewards", rewards);

        if (errorResponse != null) {
            model.addAttribute("apiError", errorResponse);
        }
        if (redemptionResponse != null) {
            model.addAttribute("redemptionSuccess", redemptionResponse);
        }
    }

    private List<RestaurantSummary> fetchRestaurants() {
        return restaurantRepository.findByActiveTrue().stream()
                .map(this::toSummary)
                .sorted(Comparator.comparing(RestaurantSummary::name, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
    }

    private List<RewardSummary> fetchRewards(Long restaurantId) {
        return rewardRepository.findByRestaurantIdAndActiveTrue(restaurantId).stream()
                .map(this::toRewardSummary)
                .toList();
    }

    private RestaurantSummary toSummary(Restaurant restaurant) {
        return new RestaurantSummary(
                restaurant.getId(),
                restaurant.getName(),
                restaurant.getCode(),
                restaurant.getDefaultCurrency());
    }

    private RewardSummary toRewardSummary(Reward reward) {
        return new RewardSummary(
                reward.getId(),
                reward.getName(),
                reward.getDescription(),
                reward.getCostPoints());
    }

    private ErrorResponse errorFromException(RuntimeException ex, HttpServletRequest request, String fallbackMessage) {
        HttpStatus status = resolveStatus(ex);
        String message = ex.getMessage() != null ? ex.getMessage() : fallbackMessage;
        return new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message, request.getRequestURI());
    }

    private HttpStatus resolveStatus(RuntimeException ex) {
        if (ex instanceof EntityNotFoundException) {
            return HttpStatus.NOT_FOUND;
        }
        if (ex instanceof IllegalArgumentException) {
            return HttpStatus.BAD_REQUEST;
        }
        if (ex instanceof IllegalStateException) {
            return HttpStatus.CONFLICT;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
