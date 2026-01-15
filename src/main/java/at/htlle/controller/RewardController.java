package at.htlle.controller;

import at.htlle.dto.AccountResponse;
import at.htlle.dto.ErrorResponse;
import at.htlle.dto.RedemptionRequest;
import at.htlle.dto.RedemptionResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
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

    private static final List<RewardCard> REWARD_CARDS = List.of(
            new RewardCard(1L, "Welcome Drink", "A free drink from the house.", 50, false),
            new RewardCard(1L, "Custom Redemption", "Use any reward id available in the backend.", 0, true)
    );

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public RewardController(RestTemplateBuilder restTemplateBuilder, ObjectMapper objectMapper) {
        this.restTemplate = restTemplateBuilder.build();
        this.objectMapper = objectMapper;
    }

    @GetMapping("/rewards")
    public String rewards(@RequestParam(name = "accountId", defaultValue = "1") Long accountId,
                          Model model,
                          HttpServletRequest request) {
        loadRewardsPage(accountId, model, request, null);
        return "rewards";
    }

    @PostMapping("/rewards/redeem")
    public String redeem(@RequestParam("accountId") Long accountId,
                         @RequestParam("rewardId") Long rewardId,
                         @RequestParam("branchId") Long branchId,
                         @RequestParam(name = "notes", required = false) String notes,
                         Model model,
                         HttpServletRequest request) {
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
            loadRewardsPage(accountId, model, request, errorResponse);
            return "rewards";
        } catch (RestClientException ex) {
            ErrorResponse errorResponse = fallbackError("Failed to redeem reward", request.getRequestURI());
            loadRewardsPage(accountId, model, request, errorResponse);
            return "rewards";
        }
    }

    private void loadRewardsPage(Long accountId, Model model, HttpServletRequest request, ErrorResponse errorResponse) {
        String baseUrl = baseUrl(request);
        try {
            AccountResponse account = restTemplate.getForObject(
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
        model.addAttribute("rewardCards", REWARD_CARDS);
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

    public record RewardCard(Long rewardId, String name, String description, long costPoints, boolean custom) {
    }
}
