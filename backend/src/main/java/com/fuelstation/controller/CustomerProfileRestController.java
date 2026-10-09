package com.fuelstation.controller;

import com.fuelstation.model.AppUser;
import com.fuelstation.service.BookingService;
import com.fuelstation.service.UserService;
import com.fuelstation.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping({"/api/v1/customer/profile", "/api/v1/account"})
public class CustomerProfileRestController {

    @Autowired
    private UserService userService;

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private BookingService bookingService;


    private String getUsername(Authentication auth) {
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return auth.getName();
        }
        return null;
    }

    @GetMapping
    public ResponseEntity<?> getProfile(Authentication auth) {
        String username = getUsername(auth);
        if (username == null) return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));

        Optional<AppUser> userOpt = userService.getUserByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "User not found"));
        }

        AppUser user = userOpt.get();
        int vehicleCount = vehicleService.getVehiclesByOwner(username).size();
        int bookingCount = bookingService.getBookingsByCustomer(username).size();

        Map<String, Object> response = new HashMap<>();
        response.put("user", user);
        response.put("stats", Map.of(
            "vehicleCount", vehicleCount,
            "bookingCount", bookingCount
        ));

        return ResponseEntity.ok(response);
    }

    @PutMapping
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> payload, Authentication auth) {
        String username = getUsername(auth);
        if (username == null) return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));

        String fullName = payload.get("fullName");
        String email = payload.get("email");
        String phoneNumber = payload.get("phoneNumber");
        String address = payload.get("address");
        String city = payload.get("city");

        userService.updateProfile(username, fullName, email, phoneNumber, address, city);

        return ResponseEntity.ok(Map.of("success", true, "message", "Profile updated successfully"));
    }

    @PatchMapping("/settings")
    public ResponseEntity<?> updateSettings(@RequestBody Map<String, Boolean> payload, Authentication auth) {
        AppUser current = userService.getUserByUsername(auth.getName()).orElseThrow(() -> new IllegalArgumentException("Account not found"));
        boolean email = payload.getOrDefault("emailNotifications", Boolean.TRUE.equals(current.getEmailNotifications()));
        boolean sms = payload.getOrDefault("smsNotifications", Boolean.TRUE.equals(current.getSmsNotifications()));
        userService.updateSettings(auth.getName(), email, sms);
        return ResponseEntity.ok(Map.of("success", true, "message", "Preferences saved"));
    }

    @PostMapping("/password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> payload, Authentication auth) {
        String username = getUsername(auth);
        if (username == null) return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));

        String currentPassword = payload.get("currentPassword");
        String newPassword = payload.get("newPassword");
        String confirmPassword = payload.get("confirmPassword");

        com.fuelstation.util.InputValidation.password(newPassword,true);

        if (!newPassword.equals(confirmPassword != null ? confirmPassword : "")) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "New password and confirmation password do not match."));
        }

        boolean success = userService.changePassword(username, currentPassword, newPassword);

        if (success) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Security credentials updated! Your new password is now active."));
        } else {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Current password verification failed. Please re-enter your password."));
        }
    }
}
