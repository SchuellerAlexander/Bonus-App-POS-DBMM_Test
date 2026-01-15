package at.htlle.controller;

import at.htlle.dto.ErrorResponse;
import at.htlle.dto.PurchaseRequest;
import at.htlle.dto.PurchaseResponse;
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

    public PurchaseController(RestTemplateBuilder restTemplateBuilder, ObjectMapper objectMapper) {
        this.restTemplate = restTemplateBuilder.build();
        this.objectMapper = objectMapper;
    }

    @GetMapping("/purchase")
    public String purchaseForm(@RequestParam(name = "accountId", defaultValue = "1") Long accountId,
                               @RequestParam(name = "branchId", required = false) Long branchId,
                               Model model) {
        model.addAttribute("accountId", accountId);
        model.addAttribute("branchId", branchId != null ? branchId : 1L);
        model.addAttribute("currency", "EUR");
        return "purchase";
    }

    @PostMapping("/purchase")
    public String createPurchase(@RequestParam("accountId") Long accountId,
                                 @RequestParam("branchId") Long branchId,
                                 @RequestParam("purchaseNumber") String purchaseNumber,
                                 @RequestParam("totalAmount") BigDecimal totalAmount,
                                 @RequestParam(name = "currency", defaultValue = "EUR") String currency,
                                 @RequestParam(name = "notes", required = false) String notes,
                                 @RequestParam(name = "description", required = false) String description,
                                 Model model,
                                 HttpServletRequest request) {
        String normalizedCurrency = currency.trim().toUpperCase(Locale.ROOT);
        PurchaseRequest payload = new PurchaseRequest(
                accountId,
                branchId,
                purchaseNumber,
                totalAmount,
                normalizedCurrency,
                null,
                notes,
                description,
                null);

        model.addAttribute("accountId", accountId);
        model.addAttribute("branchId", branchId);
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
