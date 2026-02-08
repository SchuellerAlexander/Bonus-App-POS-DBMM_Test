package at.htlle.controller;

import at.htlle.dto.ErrorResponse;
import at.htlle.dto.PurchaseRequest;
import at.htlle.dto.PurchaseResponse;
import at.htlle.dto.RestaurantSummary;
import at.htlle.entity.PointLedger;
import at.htlle.entity.Purchase;
import at.htlle.entity.Restaurant;
import at.htlle.repository.RestaurantRepository;
import at.htlle.service.AccountQueryService;
import at.htlle.service.LoyaltyService;
import at.htlle.util.SessionAccountResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.HttpStatus;

@Controller
public class PurchaseController {

    private final LoyaltyService loyaltyService;
    private final AccountQueryService accountQueryService;
    private final RestaurantRepository restaurantRepository;
    private final SessionAccountResolver sessionAccountResolver;

    public PurchaseController(LoyaltyService loyaltyService,
                              AccountQueryService accountQueryService,
                              RestaurantRepository restaurantRepository,
                              SessionAccountResolver sessionAccountResolver) {
        this.loyaltyService = loyaltyService;
        this.accountQueryService = accountQueryService;
        this.restaurantRepository = restaurantRepository;
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

        try {
            PointLedger ledger = loyaltyService.recordPurchase(payload);
            Purchase purchase = ledger.getPurchase();
            PurchaseResponse response = new PurchaseResponse(
                    purchase.getId(),
                    purchase.getPurchaseNumber(),
                    purchase.getTotalAmount(),
                    purchase.getCurrency(),
                    purchase.getPurchasedAt(),
                    ledger.getLoyaltyAccount().getId(),
                    purchase.getRestaurant().getId(),
                    ledger.getId(),
                    ledger.getPoints(),
                    ledger.getBalanceAfter());
            model.addAttribute("purchaseResponse", response);
        } catch (RuntimeException ex) {
            model.addAttribute("apiError", errorFromException(ex, request, "Failed to create purchase"));
        }
        return "purchase";
    }

    private void loadPurchaseData(Long accountId, Long restaurantId, Model model, HttpServletRequest request) {
        model.addAttribute("accountId", accountId);
        try {
            var account = accountQueryService.getAccountResponse(accountId, false);
            model.addAttribute("account", account);
            if (restaurantId == null && account != null) {
                restaurantId = account.restaurantId();
            }
        } catch (RuntimeException ex) {
            model.addAttribute("apiError", errorFromException(ex, request, "Failed to load account"));
        }

        List<RestaurantSummary> restaurants = fetchRestaurants();
        model.addAttribute("restaurants", restaurants);

        if (restaurantId == null && restaurants.size() == 1) {
            restaurantId = restaurants.get(0).id();
        }
        model.addAttribute("selectedRestaurantId", restaurantId);
        model.addAttribute("currency", resolveCurrency(restaurants, restaurantId, "EUR"));
    }

    private List<RestaurantSummary> fetchRestaurants() {
        return restaurantRepository.findByActiveTrue().stream()
                .map(this::toSummary)
                .sorted(Comparator.comparing(RestaurantSummary::name, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
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

    private RestaurantSummary toSummary(Restaurant restaurant) {
        return new RestaurantSummary(
                restaurant.getId(),
                restaurant.getName(),
                restaurant.getCode(),
                restaurant.getDefaultCurrency());
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
