package at.htlle.controller;

import at.htlle.dto.AccountResponse;
import at.htlle.dto.ErrorResponse;
import at.htlle.dto.PurchaseRequest;
import at.htlle.dto.PurchaseResponse;
import at.htlle.dto.RestaurantSummaryResponse;
import at.htlle.util.SessionAccountResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.Instant;
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
        loadPurchasePage(accountId, model, request, restaurantId, "EUR");
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
                null
        );

        loadPurchasePage(accountId, model, request, restaurantId, normalizedCurrency);

        try {
            PurchaseResponse response = restTemplate.postForObject(
                    baseUrl(request) + "/api/purchases",
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

    private void loadPurchasePage(Long accountId,
                                  Model model,
                                  HttpServletRequest request,
                                  Long restaurantId,
                                  String currency) {

        model.addAttribute("accountId", accountId);
        model.addAttribute("currency", currency);

        AccountResponse account = null;
        try {
            account = restTemplate.getForObject(
                    baseUrl(request) + "/api/accounts/{id}?includeLedger=false",
                    AccountResponse.class,
                    accountId);
        } catch (Exception ex) {
            model.addAttribute("apiError", fallbackError("Failed to load account", request.getRequestURI()));
        }

        RestaurantSummaryResponse[] restaurants = new RestaurantSummaryResponse[0];
        try {
            restaurants = restTemplate.getForObject(
                    baseUrl(request) + "/api/restaurants",
                    RestaurantSummaryResponse[].class);
        } catch (Exception ex) {
            model.addAttribute("apiError", fallbackError("Failed to load restaurants", request.getRequestURI()));
        }

        model.addAttribute("restaurants", restaurants);

        Long selectedId = restaurantId;
        if (selectedId == null && account != null) {
            selectedId = account.restaurantId();
        }
        if (selectedId == null && restaurants != null && restaurants.length > 0) {
            selectedId = restaurants[0].id();
        }

        model.addAttribute("restaurantId", selectedId);
    }

    private String baseUrl(HttpServletRequest request) {
        return ServletUriComponentsBuilder.fromContextPath(request).build().toUriString();
    }

    private ErrorResponse parseError(HttpStatusCodeException ex, HttpServletRequest request) {
        try {
            return objectMapper.readValue(ex.getResponseBodyAsByteArray(), ErrorResponse.class);
        } catch (Exception e) {
            return fallbackError(ex.getStatusText(), request.getRequestURI());
        }
    }

    private ErrorResponse fallbackError(String message, String path) {
        return new ErrorResponse(Instant.now(), 500, "Internal Server Error", message, path);
    }
}
