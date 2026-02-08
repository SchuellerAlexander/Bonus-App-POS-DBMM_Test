package at.htlle.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            // CSRF aktiv lassen (für Thymeleaf + Form-Login korrekt)
            .csrf(AbstractHttpConfigurer::disable)

            .authorizeHttpRequests(auth -> auth
                // öffentliche Seiten
                .requestMatchers(
                        "/",
                        "/login",
                        "/signup",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/h2-console/**"
                ).permitAll()

                // Admin-Bereich
                .requestMatchers("/admin/**").hasRole("ADMIN")

                // User-Bereich
                .requestMatchers(
                        "/dashboard",
                        "/purchase",
                        "/rewards"
                ).hasRole("CUSTOMER")

                // ALLES ANDERE
                .anyRequest().authenticated()
            )

            // Login
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
                .permitAll()
            )

            // Logout
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
            )

            // H2 Console
            .headers(headers -> headers.frameOptions(frame -> frame.disable()));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
