package at.htlle.controller;

import at.htlle.entity.LoyaltyAccount;
import at.htlle.service.AuthService;
import at.htlle.util.SessionAccountResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private final AuthService authService;
    private final SessionAccountResolver sessionAccountResolver;

    public AuthController(AuthService authService, SessionAccountResolver sessionAccountResolver) {
        this.authService = authService;
        this.sessionAccountResolver = sessionAccountResolver;
    }

    @GetMapping("/login")
    public String login(Model model, HttpServletRequest request) {
        Long accountId = sessionAccountResolver.getAccountId(request);
        if (accountId != null) {
            return "redirect:/dashboard";
        }
        model.addAttribute("hideChrome", true);
        return "login";
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam("username") String username,
                          @RequestParam("password") String password,
                          Model model,
                          HttpServletRequest request) {
        return authService.authenticate(username, password)
                .map(account -> {
                    sessionAccountResolver.setAccountId(request, account.getId());
                    return "redirect:/dashboard";
                })
                .orElseGet(() -> {
                    model.addAttribute("loginError", "Invalid username or password");
                    model.addAttribute("hideChrome", true);
                    return "login";
                });
    }

    @PostMapping("/signup")
    public String signup(@RequestParam("firstName") String firstName,
                         @RequestParam("lastName") String lastName,
                         @RequestParam("email") String email,
                         @RequestParam("username") String username,
                         @RequestParam("password") String password,
                         Model model,
                         HttpServletRequest request) {
        try {
            LoyaltyAccount account = authService.register(firstName, lastName, email, username, password);
            sessionAccountResolver.setAccountId(request, account.getId());
            return "redirect:/dashboard";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("signupError", ex.getMessage());
            model.addAttribute("hideChrome", true);
            return "login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request) {
        sessionAccountResolver.clear(request);
        return "redirect:/login";
    }
}
