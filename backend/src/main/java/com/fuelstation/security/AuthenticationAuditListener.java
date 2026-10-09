package com.fuelstation.security;

import com.fuelstation.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationAuditListener {

    @Autowired
    private AuditLogService auditLogService;

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        try {
            Authentication auth = event.getAuthentication();
            String username = auth.getName();
            String role = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .findFirst()
                    .orElse("USER")
                    .replace("ROLE_", "");

            auditLogService.record(
                    "LOGIN_SUCCESS",
                    username,
                    role,
                    "User " + username + " authenticated successfully into FuelCore OS",
                    "SUCCESS"
            );
        } catch (Exception ignored) {
            // Never break authentication on audit log logging failure
        }
    }

    @EventListener
    public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        try {
            String username = (event.getAuthentication() != null) ? event.getAuthentication().getName() : "Unknown";
            String reason = (event.getException() != null) ? event.getException().getMessage() : "Bad credentials";

            auditLogService.record(
                    "LOGIN_FAILED",
                    username,
                    "Unknown",
                    "Authentication failed for user: " + username + " (Reason: " + reason + ")",
                    "FAILED"
            );
        } catch (Exception ignored) {
            // Never break authentication on audit log failure
        }
    }
}
