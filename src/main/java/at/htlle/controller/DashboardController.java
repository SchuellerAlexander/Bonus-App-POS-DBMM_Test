package at.htlle.controller;

import at.htlle.dto.AccountResponse;
import at.htlle.dto.ErrorResponse;
import at.htlle.service.AuthService;
import at.htlle.util.SessionAccountResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Controller
public class DashboardController {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final SessionAccountResolver sessionAccountResolver;
    private final AuthService authService;

    public DashboardController(RestTemplateBuilder restTemplateBuilder,
                               ObjectMapper objectMapper,
                               SessionAccountResolver sessionAccountResolver,
                               AuthService authService) {
        this.restTemplate = restTemplateBuilder.build();
        this.objectMapper = objectMapper;
        this.sessionAccountResolver = sessionAccountResolver;
        this.authService = authService;
    }

    @GetMapping("/")
    public String home(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(auth -> "ROLE_ADMIN".equals(auth.getAuthority()));
            return isAdmin ? "redirect:/admin" : "redirect:/dashboard";
        }
        return "redirect:/login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpServletRequest request, Authentication authentication) {
        Long accountId = sessionAccountResolver.getAccountId(request);
        if (accountId == null && authentication != null) {
            accountId = authService.resolveAccountId(authentication.getName())
                    .orElse(null);
            if (accountId != null) {
                sessionAccountResolver.setAccountId(request, accountId);
            }
        }
        if (accountId == null) {
            return "redirect:/login";
        }
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
