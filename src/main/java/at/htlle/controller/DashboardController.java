package at.htlle.controller;

import at.htlle.dto.AccountResponse;
import at.htlle.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Controller
public class DashboardController {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public DashboardController(RestTemplateBuilder restTemplateBuilder, ObjectMapper objectMapper) {
        this.restTemplate = restTemplateBuilder.build();
        this.objectMapper = objectMapper;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(name = "accountId", defaultValue = "1") Long accountId,
                            Model model,
                            HttpServletRequest request) {
        String baseUrl = baseUrl(request);
        try {
            AccountResponse account = restTemplate.getForObject(
                    baseUrl + "/api/accounts/{id}?includeLedger=true",
                    AccountResponse.class,
                    accountId);
            model.addAttribute("account", account);
        } catch (HttpStatusCodeException ex) {
            model.addAttribute("apiError", parseError(ex, request));
        } catch (RestClientException ex) {
            model.addAttribute("apiError", fallbackError("Failed to load account", request.getRequestURI()));
        }
        model.addAttribute("accountId", accountId);
        return "dashboard";
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
