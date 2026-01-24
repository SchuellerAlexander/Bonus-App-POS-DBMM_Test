package at.htlle.controller;

import at.htlle.dto.AccountResponse;
import at.htlle.dto.ErrorResponse;
import at.htlle.dto.PurchaseRequest;
import at.htlle.dto.PurchaseResponse;
import at.htlle.dto.RestaurantSummary;
import at.htlle.util.SessionAccountResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
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
public class PurchaseController {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final SessionAccountResolver sessionAccountResolver;

    public PurchaseController(RestTemplateBuilder restTemplateBuilder,
                              ObjectMapper objectMapper,
                              SessionAccountResolver sessionAccountResolver) {
        this.restTemplate = restTemplateBuilder.build();
        this.objectMapper = objectMapper;
        this.sessionAccountResolver = sessionAccountResolver;
    }

    @GetMapping("/purchase")
    public String purchaseForm(@RequestParam(name = "restaurantId", required = false) Long restaurantId,
                               Model model,
                               HttpServletRequest request) {
        Long accountId = sessionAccountResolver.getAccountId(request);
        if (accountId == null) {
            return "redirect:/login";
        }
        loadPurchaseData(accountId, restaurantId, model, request);
        return "purchase";
    }

    @PostMapping("/purchase")
    public String createPurchase(@RequestParam("restaurantId") Long restaurantId,
                                 @RequestParam("purchaseNumber") String purchaseNumber,
                                 @RequestParam("totalAmount") BigDecimal totalAmount,
                                 @RequestParam(name = "currency", defaultValue = "EUR") String currency,
                                 @RequestParam(name = "notes", required = false) String notes,
                                 @RequestParam(name = "description", required = false) String description,
                                 Model model,
                                 HttpServletRequest request) {
        Long accountId = sessionAccountResolver.getAccountId(request);
        if (accountId == null) {
            return "redirect:/login";
        }
        String normalizedCurrency = currency.trim().toUpperCase(Locale.ROOT);
        PurchaseRequest payload = new PurchaseRequest(
                accountId,
                restaurantId,
                purchaseNumber,
                totalAmount,
                normalizedCurrency,
                null,
                notes,
                description,
                null);

        loadPurchaseData(accountId, restaurantId, model, request);
        model.addAttribute("currency", normalizedCurrency);

        String baseUrl = baseUrl(request);
        try {
            PurchaseResponse response = restTemplate.postForObject(
                    baseUrl + "/api/purchases",
                    payload,
                    PurchaseResponse.class);
            model.addAttribute("purchaseResponse", response);
        } catch (HttpStatusCodeException ex) {
            model.addAttribute("apiError", parseError(ex, request));
        } catch (RestClientException ex) {
            model.addAttribute("apiError", fallbackError("Failed to create purchase", request.getRequestURI()));
        }
        return "purchase";
    }

    private void loadPurchaseData(Long accountId, Long restaurantId, Model model, HttpServletRequest request) {
        model.addAttribute("accountId", accountId);
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

        List<RestaurantSummary> restaurants = fetchRestaurants(baseUrl);
        model.addAttribute("restaurants", restaurants);

        if (restaurantId == null && restaurants.size() == 1) {
            restaurantId = restaurants.get(0).id();
        }
        model.addAttribute("selectedRestaurantId", restaurantId);
        model.addAttribute("currency", resolveCurrency(restaurants, restaurantId, "EUR"));
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

    private String resolveCurrency(List<RestaurantSummary> restaurants, Long restaurantId, String fallback) {
        if (restaurantId == null) {
            return fallback;
        }
        return restaurants.stream()
                .filter(restaurant -> restaurant.id().equals(restaurantId))
                .map(RestaurantSummary::defaultCurrency)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(fallback);
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
