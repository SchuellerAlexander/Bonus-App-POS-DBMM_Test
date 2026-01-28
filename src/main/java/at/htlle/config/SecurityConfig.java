package at.htlle.config;

import at.htlle.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.io.IOException;
import java.util.Set;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthenticationSuccessHandler authenticationSuccessHandler,
            Environment environment
    ) throws Exception {

        boolean devProfileActive = environment.acceptsProfiles(Profiles.of("dev"));

        http
            // CSRF
            .csrf(csrf -> {
                if (devProfileActive) {
                    csrf.ignoringRequestMatchers(PathRequest.toH2Console());
                }
            })

            // AUTHORIZATION (ALLES IN EINEM BLOCK!)
            .authorizeHttpRequests(auth -> {
                auth
                    // public
                    .requestMatchers(
                        "/login",
                        "/signup",
                        "/css/**",
                        "/js/**",
                        "/images/**"
                    ).permitAll()

                    // H2 Console (nur dev)
                    .requestMatchers(PathRequest.toH2Console()).permitAll()

                    // admin
                    .requestMatchers("/admin/**").hasRole("ADMIN")

                    // user
                    .requestMatchers(
                        "/dashboard",
                        "/purchase",
                        "/rewards",
                        "/api/**"
                    ).hasRole("USER")

                    // ALLES ANDERE
                    .anyRequest().authenticated();
            })

            // HTTP Basic aus
            .httpBasic(AbstractHttpConfigurer::disable)

            // Login
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .failureUrl("/login?error")
                .successHandler(authenticationSuccessHandler)
                .permitAll()
            )

            // Logout
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "POST"))
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );

        // H2 Frame-Options (nur dev)
        if (devProfileActive) {
            http.headers(headers -> headers.frameOptions(frame -> frame.disable()));
        }

        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler(AuthService authService) {
        return new AuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(
                    HttpServletRequest request,
                    HttpServletResponse response,
                    org.springframework.security.core.Authentication authentication
            ) throws IOException, ServletException {

                Set<String> roles =
                        AuthorityUtils.authorityListToSet(authentication.getAuthorities());
                String username = authentication.getName();

                if (roles.contains("ROLE_USER")) {
                    Long accountId = authService.resolveAccountId(username)
                        .orElseThrow(() ->
                            new IllegalStateException("No loyalty account available for user"));
                    request.getSession(true).setAttribute("accountId", accountId);
                }

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


