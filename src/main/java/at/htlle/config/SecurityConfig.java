package at.htlle.config;

import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import at.htlle.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   AuthenticationSuccessHandler authenticationSuccessHandler) throws Exception {
        http
                .csrf(csrf -> csrf.ignoringRequestMatchers(PathRequest.toH2Console()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PathRequest.toH2Console()).permitAll()
                        .requestMatchers("/login", "/signup", "/css/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/dashboard", "/purchase", "/rewards", "/api/**").hasRole("USER")
                        .anyRequest().authenticated())
                .headers(headers -> headers.frameOptions(frame -> frame.disable()))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .failureUrl("/login?error")
                        .successHandler(authenticationSuccessHandler)
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .permitAll());
        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler(AuthService authService) {
        return new AuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(HttpServletRequest request,
                                                HttpServletResponse response,
                                                org.springframework.security.core.Authentication authentication)
                    throws IOException, ServletException {
                Set<String> roles = org.springframework.security.core.authority.AuthorityUtils
                        .authorityListToSet(authentication.getAuthorities());
                String username = authentication.getName();
                authService.resolveAccountId(username)
                        .ifPresent(accountId -> request.getSession(true).setAttribute("accountId", accountId));
                if (roles.contains("ROLE_ADMIN")) {
                    response.sendRedirect("/admin");
                } else {
                    response.sendRedirect("/dashboard");
                }
            }
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
