package com.fuelstation.controller;

import com.fuelstation.model.AppUser;
import com.fuelstation.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthRestController {
    @Autowired private UserService users;
    @Autowired private com.fuelstation.service.PasswordRecoveryService recovery;
    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) return ResponseEntity.status(401).body(Map.of("message", "Please sign in"));
        AppUser user = users.getUserByUsername(auth.getName()).orElseThrow();
        String role = auth.getAuthorities().stream().map(a -> a.getAuthority()).filter(a -> a.startsWith("ROLE_")).findFirst().orElseThrow();
        return ResponseEntity.ok(Map.of("user", user, "role", role));
    }
    public record Signup(String fullName, String username, String email, String phoneNumber, String password, String confirmPassword) {}
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody Signup form) {
        if (form.password() == null || !form.password().equals(form.confirmPassword())) throw new IllegalArgumentException("Passwords do not match");
        AppUser user = new AppUser(); user.setUsername(form.username()); user.setFullName(form.fullName()); user.setEmail(form.email()); user.setPhoneNumber(form.phoneNumber()); user.setPassword(form.password()); user.setRole("Customer"); user.setStatus("Active"); users.createUser(user);
        return ResponseEntity.ok(Map.of("success", true, "message", "Account created. Sign in to continue."));
    }
    public record Recovery(String identifier) {}
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgot(@RequestBody Recovery form) {
        recovery.request(form.identifier());
        return ResponseEntity.ok(Map.of("success", true, "message", "If an active account matches, a reset link has been sent to its registered email address."));
    }
    public record Reset(String token, String newPassword, String confirmPassword) {}
    @PostMapping("/reset-password")
    public ResponseEntity<?> reset(@RequestBody Reset form) {
        if (form.newPassword() == null || form.newPassword().codePointCount(0,form.newPassword().length()) < 15 || !form.newPassword().equals(form.confirmPassword())) throw new IllegalArgumentException("Enter matching passwords with at least 15 characters");
        if (!users.resetPasswordWithToken(form.token(), form.newPassword())) throw new IllegalArgumentException("Reset link is invalid or expired");
        return ResponseEntity.ok(Map.of("success", true, "message", "Password updated. Sign in using your new password."));
    }
}
