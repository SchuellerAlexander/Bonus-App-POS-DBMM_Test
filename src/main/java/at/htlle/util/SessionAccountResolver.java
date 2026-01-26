package at.htlle.util;

import at.htlle.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SessionAccountResolver {

    private final AuthService authService;

    public SessionAccountResolver(AuthService authService) {
        this.authService = authService;
    }

    public Long getAccountId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Long accountId = extractAccountId(session);
        if (accountId != null) {
            return accountId;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return authService.resolveAccountId(authentication.getName())
                .map(resolved -> {
                    request.getSession(true).setAttribute("accountId", resolved);
                    return resolved;
                })
                .orElse(null);
    }

    public void setAccountId(HttpServletRequest request, Long accountId) {
        request.getSession(true).setAttribute("accountId", accountId);
    }

    public void clear(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    private Long extractAccountId(HttpSession session) {
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute("accountId");
        if (value instanceof Long accountId) {
            return accountId;
        }
        if (value instanceof String accountIdText) {
            try {
                return Long.parseLong(accountIdText);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
