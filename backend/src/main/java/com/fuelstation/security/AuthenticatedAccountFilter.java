package com.fuelstation.security;

import com.fuelstation.repository.AppUserRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Locale;

public class AuthenticatedAccountFilter extends OncePerRequestFilter {
    private final AppUserRepository users;
    public AuthenticatedAccountFilter(AppUserRepository users) { this.users = users; }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            var user = users.findByUsername(auth.getName()).orElse(null);
            boolean active = user != null && "Active".equalsIgnoreCase(user.getStatus());
            String role = user == null || user.getRole() == null ? "" : "ROLE_" + user.getRole().toUpperCase(Locale.ROOT).replace("ROLE_", "");
            boolean unchanged = auth.getAuthorities().stream().anyMatch(a -> role.equals(a.getAuthority()));
            var authenticatedSession=request.getSession(false);
            boolean samePassword=user!=null && authenticatedSession!=null && java.util.Objects.equals(authenticatedSession.getAttribute("fuelcore.credentials"),user.getPassword());
            if (!active || !unchanged || !samePassword) {
                var session = request.getSession(false); if (session != null) session.invalidate();
                SecurityContextHolder.clearContext(); response.setStatus(401); response.setContentType("application/json");
                response.getWriter().write("{\"message\":\"Your account access changed. Please sign in again.\"}"); return;
            }
        }
        chain.doFilter(request, response);
    }
}
