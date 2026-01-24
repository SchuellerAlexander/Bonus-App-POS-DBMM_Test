package at.htlle.controller;

import at.htlle.dto.AccountResponse;
import at.htlle.dto.ErrorResponse;
import at.htlle.dto.RedemptionRequest;
import at.htlle.dto.RedemptionResponse;
import at.htlle.dto.RestaurantSummaryResponse;
import at.htlle.dto.RewardSummaryResponse;
import at.htlle.util.SessionAccountResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Controller
public class RewardController {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final SessionAccountResolver sessionAccountResolver;

    public RewardController(RestTemplateBuilder restTemplateBuilder,
                            ObjectMapper objectMapper,
                            SessionAccountResolver sessionAccountResolver) {
        this.restTemplate = restTemplateBuilder.build();
        this.objectMapper = objectMapper;
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
        loadRewardsPage(accountId, model, request, restaurantId, null);
        return "rewards";
    }

    @PostMapping("/rewards/redeem")
    public String redeem(@RequestParam("rewardId") Long rewardId,
                         @RequestParam("branchId") Long branchId,
                         @RequestParam("restaurantId") Long restaurantId,
                         @RequestParam(name = "notes", required = false) String notes,
                         Model model,
                         HttpServletRequest request) {
        Long accountId = sessionAccountResolver.getAccountId(request);
        if (accountId == null) {
            return "redirect:/login";
        }
        RedemptionRequest payload = new RedemptionRequest(accountId, rewardId, branchId, notes);
        String baseUrl = baseUrl(request);
        try {
            RedemptionResponse response = restTemplate.postForObject(
                    baseUrl + "/api/redemptions",
                    payload,
                    RedemptionResponse.class);
            model.addAttribute("redemption", response);
            model.addAttribute("accountId", accountId);
            return "redemption-success";
        } catch (HttpStatusCodeException ex) {
            ErrorResponse errorResponse = parseError(ex, request);
            loadRewardsPage(accountId, model, request, restaurantId, errorResponse);
            return "rewards";
        } catch (RestClientException ex) {
            ErrorResponse errorResponse = fallbackError("Failed to redeem reward", request.getRequestURI());
            loadRewardsPage(accountId, model, request, restaurantId, errorResponse);
            return "rewards";
        }
    }

    private void loadRewardsPage(Long accountId,
                                 Model model,
                                 HttpServletRequest request,
                                 Long restaurantId,
                                 ErrorResponse errorResponse) {
        String baseUrl = baseUrl(request);
        AccountResponse account = null;
        try {
            account = restTemplate.getForObject(
                    baseUrl + "/api/accounts/{id}?includeLedger=false",
                    AccountResponse.class,
                    accountId);
            model.addAttribute("account", account);
        } catch (HttpStatusCodeException ex) {
            model.addAttribute("apiError", parseError(ex, request));
        } catch (RestClientException ex) {
            model.addAttribute("apiError", fallbackError("Failed to load account", request.getRequestURI()));
        }
        model.addAttribute("accountId", accountId);
        RestaurantSummaryResponse[] restaurants = new RestaurantSummaryResponse[0];
        try {
            RestaurantSummaryResponse[] response = restTemplate.getForObject(
                    baseUrl + "/api/restaurants",
                    RestaurantSummaryResponse[].class);
            restaurants = Objects.requireNonNullElse(response, new RestaurantSummaryResponse[0]);
        } catch (HttpStatusCodeException ex) {
            model.addAttribute("apiError", parseError(ex, request));
        } catch (RestClientException ex) {
            model.addAttribute("apiError", fallbackError("Failed to load restaurants", request.getRequestURI()));
        }

        model.addAttribute("restaurants", restaurants);
        Long selectedRestaurantId = Optional.ofNullable(restaurantId)
                .orElseGet(() -> account != null ? account.restaurantId() : null);
        if (selectedRestaurantId == null && restaurants.length > 0) {
            selectedRestaurantId = restaurants[0].id();
        }

        model.addAttribute("restaurantId", selectedRestaurantId);
        Long defaultBranchId = null;
        if (selectedRestaurantId != null) {
            for (RestaurantSummaryResponse restaurant : restaurants) {
                if (restaurant.id().equals(selectedRestaurantId)) {
                    defaultBranchId = restaurant.defaultBranchId();
                    break;
                }
            }
        }
        model.addAttribute("defaultBranchId", defaultBranchId);

        List<RewardSummaryResponse> rewards = List.of();
        if (selectedRestaurantId != null) {
            try {
                RewardSummaryResponse[] rewardResponse = restTemplate.getForObject(
                        baseUrl + "/api/restaurants/{id}/rewards",
                        RewardSummaryResponse[].class,
                        selectedRestaurantId);
                rewards = rewardResponse != null ? Arrays.asList(rewardResponse) : List.of();
            } catch (HttpStatusCodeException ex) {
                model.addAttribute("apiError", parseError(ex, request));
            } catch (RestClientException ex) {
                model.addAttribute("apiError", fallbackError("Failed to load rewards", request.getRequestURI()));
            }
        }
        model.addAttribute("rewards", rewards);
        if (errorResponse != null) {
            model.addAttribute("apiError", errorResponse);
        }
    }

    private String baseUrl(HttpServletRequest request) {
        return ServletUriComponentsBuilder.fromContextPath(request).build().toUriString();
    }

    private ErrorResponse parseError(HttpStatusCodeException ex, HttpServletRequest request) {
        try {
            return objectMapper.readValue(ex.getResponseBodyAsByteArray(), ErrorResponse.class);
        } catch (Exception parseEx) {
            return fallbackError(ex.getStatusText(), request.getRequestURI());
        }
    }

    private ErrorResponse fallbackError(String message, String path) {
        return new ErrorResponse(Instant.now(), 500, "Internal Server Error", message, path);
    }

}
