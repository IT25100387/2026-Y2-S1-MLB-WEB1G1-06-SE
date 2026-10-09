package com.fuelstation.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Allow the React dev server to communicate with this backend
        configuration.setAllowedOriginPatterns(Arrays.asList("http://localhost:*", "http://127.0.0.1:*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "Idempotency-Key"));
        configuration.setAllowCredentials(true); // Needed for JSESSIONID cookies
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, DaoAuthenticationProvider authenticationProvider, com.fuelstation.repository.AppUserRepository users, com.fasterxml.jackson.databind.ObjectMapper json) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authenticationProvider(authenticationProvider)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/api/auth/login", "/api/auth/signup", "/api/auth/forgot-password", "/api/auth/reset-password", "/images/**", "/favicon.ico", "/login", "/signup", "/forgot-password", "/reset-password", "/error", "/api/v1/customer/feedback/public").permitAll()
                .requestMatchers("/api/auth/me", "/api/v1/account/**", "/api/v1/notifications/**").authenticated()
                .requestMatchers("/api/fuel-operations/receipts", "/api/fuel-operations/receipts/**", "/api/v1/customer/fuel/*/receipt").denyAll()
                .requestMatchers("/api/v1/customer/**").hasRole("CUSTOMER")
                .requestMatchers("/api/v1/mechanic/**").hasRole("MECHANIC")
                .requestMatchers("/api/v1/users/**", "/users/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/audit-logs/**", "/audit-logs/**", "/admin/audit-logs/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/customers", "/customers/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers("/api/v1/hr/self/clock-in", "/api/v1/hr/self/clock-out", "/api/v1/hr/attendance/**", "/attendance/**").denyAll()
                .requestMatchers("/api/v1/hr/self/**").hasAnyRole("MANAGER", "CASHIER", "MECHANIC")
                .requestMatchers("/api/v1/hr/**", "/api/v1/support/**", "/api/v1/feedback/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/pos/card/notify").permitAll()
                .requestMatchers("/api/pos/**").hasRole("CASHIER")
                .requestMatchers("/api/payments/card/**").hasAnyRole("ADMIN", "MANAGER", "CASHIER", "CUSTOMER")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/billing/payments").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers("/api/billing/**").hasAnyRole("ADMIN", "MANAGER", "CASHIER")
                .requestMatchers("/api/workshop/bookings/**", "/api/workshop/vehicles/**").hasAnyRole("MANAGER", "CASHIER")
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/workshop/job/update/*").hasAnyRole("MANAGER", "MECHANIC")
                .requestMatchers("/api/workshop/**").hasRole("MANAGER")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/inventory/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers("/api/inventory/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/fuel-operations/prices").hasAnyRole("MANAGER", "CASHIER")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/services").hasAnyRole("ADMIN", "MANAGER", "CASHIER")
                .requestMatchers("/api/fuel-operations/**").hasRole("MANAGER")
                .requestMatchers("/api/services/**", "/api/offers/**", "/admin/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers("/api/dashboard/admin-overview").hasRole("ADMIN")
                .requestMatchers("/api/dashboard/**").hasAnyRole("ADMIN", "MANAGER", "CASHIER")
                .requestMatchers("/api/**").denyAll()
                .requestMatchers("/customer/**").hasRole("CUSTOMER")
                .requestMatchers("/mechanic/**").hasAnyRole("MANAGER", "MECHANIC")
                .requestMatchers("/staff/**", "/shifts/**", "/leave/**", "/feedback/**", "/admin_support/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers("/workshop/**", "/attendance/**").hasRole("MANAGER")
                .requestMatchers("/prices/**").hasAnyRole("MANAGER", "CASHIER", "CUSTOMER")
                .requestMatchers("/inventory/**", "/spare-parts/**", "/suppliers/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers("/sales/**").hasRole("MANAGER")
                .requestMatchers("/invoices/**", "/payments/**").hasAnyRole("ADMIN", "MANAGER", "CASHIER")
                .requestMatchers("/vehicles/**", "/bookings/**", "/service-history/**", "/reports/**").hasAnyRole("MANAGER", "CASHIER")
                .requestMatchers("/h2-console/**", "/switch-role").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .exceptionHandling(e -> e
                // Return 401 Unauthorized JSON instead of redirecting to /login HTML page
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"status\":\"error\",\"message\":\"Unauthorized access\"}");
                })
            )
            .formLogin(form -> form
                .loginPage("/login") // The page route redirects to the React sign-in screen.
                .loginProcessingUrl("/api/auth/login") // React will POST here
                .successHandler((request, response, authentication) -> {
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setContentType("application/json");
                    // Return user roles so React knows which dashboard to route to
                    String role = authentication.getAuthorities().stream().map(a -> a.getAuthority()).filter(a -> a.startsWith("ROLE_")).findFirst().orElse("ROLE_CUSTOMER");
                    var account = users.findByUsername(authentication.getName()).orElseThrow();
                    request.getSession().setAttribute("fuelcore.credentials",account.getPassword());
                    json.writeValue(response.getWriter(), java.util.Map.of("status", "success", "role", role, "username", account.getUsername(), "fullName", account.getFullName() == null ? account.getUsername() : account.getFullName(), "email", account.getEmail() == null ? "" : account.getEmail()));
                })
                .failureHandler((request, response, exception) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"status\":\"error\",\"message\":\"Invalid credentials\"}");
                })
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler((request, response, authentication) -> {
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"status\":\"success\",\"message\":\"Logged out\"}");
                })
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .csrf(csrf -> csrf.disable());

        http.addFilterAfter(new AuthenticatedAccountFilter(users), org.springframework.security.web.authentication.AnonymousAuthenticationFilter.class);
        return http.build();
    }
}
