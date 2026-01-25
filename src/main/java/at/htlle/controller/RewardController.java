package at.htlle.controller;

import at.htlle.dto.AccountResponse;
import at.htlle.dto.ErrorResponse;
import at.htlle.dto.RedemptionResponse;
import at.htlle.dto.RewardRedeemRequest;
import at.htlle.dto.RestaurantSummary;
import at.htlle.dto.RewardSummary;
import at.htlle.util.SessionAccountResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
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
        RewardRedeemRequest payload = new RewardRedeemRequest(notes);
        String baseUrl = baseUrl(request);
        try {
            RedemptionResponse response = restTemplate.postForObject(
                    baseUrl + "/api/rewards/{id}/redeem",
                    payload,
                    RedemptionResponse.class,
                    rewardId);
            loadRewardsPage(accountId, restaurantId, model, request, null, response);
            return "rewards";
        } catch (HttpStatusCodeException ex) {
            ErrorResponse errorResponse = parseError(ex, request);
            loadRewardsPage(accountId, restaurantId, model, request, errorResponse, null);
            return "rewards";
        } catch (RestClientException ex) {
            ErrorResponse errorResponse = fallbackError("Failed to redeem reward", request.getRequestURI());
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
        String baseUrl = baseUrl(request);
        try {
            AccountResponse account = restTemplate.getForObject(
                    baseUrl + "/api/accounts/{id}?includeLedger=false",
                    AccountResponse.class,
                    accountId);
            model.addAttribute("account", account);
            if (restaurantId == null && account != null) {
                restaurantId = account.restaurantId();
            }
        } catch (HttpStatusCodeException ex) {
            model.addAttribute("apiError", parseError(ex, request));
        } catch (RestClientException ex) {
            model.addAttribute("apiError", fallbackError("Failed to load account", request.getRequestURI()));
        }
        model.addAttribute("accountId", accountId);

        List<RestaurantSummary> restaurants = fetchRestaurants(baseUrl);
        model.addAttribute("restaurants", restaurants);
        if (restaurantId == null && restaurants.size() == 1) {
            restaurantId = restaurants.get(0).id();
        }
        model.addAttribute("selectedRestaurantId", restaurantId);

        List<RewardSummary> rewards = List.of();
        if (restaurantId != null) {
            rewards = fetchRewards(baseUrl, restaurantId);
        }
        model.addAttribute("rewards", rewards);

        if (errorResponse != null) {
            model.addAttribute("apiError", errorResponse);
        }
        if (redemptionResponse != null) {
            model.addAttribute("redemptionSuccess", redemptionResponse);
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

    private List<RestaurantSummary> fetchRestaurants(String baseUrl) {
        try {
            RestaurantSummary[] response = restTemplate.getForObject(
                    baseUrl + "/api/restaurants",
                    RestaurantSummary[].class);
            if (response == null) {
                return List.of();
            }
            return Arrays.stream(response)
                    .sorted(Comparator.comparing(RestaurantSummary::name, Comparator.nullsLast(String::compareToIgnoreCase)))
                    .toList();
        } catch (RestClientException ex) {
            return List.of();
        }
    }

    private List<RewardSummary> fetchRewards(String baseUrl, Long restaurantId) {
        try {
            RewardSummary[] response = restTemplate.getForObject(
                    baseUrl + "/api/restaurants/{id}/rewards",
                    RewardSummary[].class,
                    restaurantId);
            if (response == null) {
                return List.of();
            }
            return Arrays.asList(response);
        } catch (RestClientException ex) {
            return List.of();
        }
    }
}
