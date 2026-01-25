package at.htlle.controller;

import at.htlle.service.AuthService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/login")
    public String login(Model model,
                        Authentication authentication,
                        @RequestParam(name = "error", required = false) String error) {
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(auth -> "ROLE_ADMIN".equals(auth.getAuthority()));
            return isAdmin ? "redirect:/admin" : "redirect:/dashboard";
        }
        if (error != null) {
            model.addAttribute("loginError", "Invalid username or password");
        }
        return "login";
    }

    @PostMapping("/signup")
    public String signup(@RequestParam("firstName") String firstName,
                         @RequestParam("lastName") String lastName,
                         @RequestParam("email") String email,
                         @RequestParam("username") String username,
                         @RequestParam("password") String password,
                         Model model) {
        try {
            authService.register(firstName, lastName, email, username, password);
            return "redirect:/login";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("signupError", ex.getMessage());
            return "login";
        }
    }
}
