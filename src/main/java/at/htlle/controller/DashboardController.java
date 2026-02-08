package at.htlle.controller;

import at.htlle.dto.ErrorResponse;
import at.htlle.util.SessionAccountResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.HttpStatus;
import at.htlle.service.AccountQueryService;

@Controller
public class DashboardController {

    private final AccountQueryService accountQueryService;
    private final SessionAccountResolver sessionAccountResolver;

    public DashboardController(AccountQueryService accountQueryService,
                               SessionAccountResolver sessionAccountResolver) {
        this.accountQueryService = accountQueryService;
        this.sessionAccountResolver = sessionAccountResolver;
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
    public String dashboard(Model model, HttpServletRequest request) {
        Long accountId = sessionAccountResolver.getAccountId(request);
        if (accountId == null) {
            return "redirect:/login";
        }
        try {
            var account = accountQueryService.getAccountResponse(accountId, true);
            model.addAttribute("account", account);
        } catch (RuntimeException ex) {
            model.addAttribute("apiError", errorFromException(ex, request, "Failed to load account"));
        }
        model.addAttribute("accountId", accountId);
        return "dashboard";
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
