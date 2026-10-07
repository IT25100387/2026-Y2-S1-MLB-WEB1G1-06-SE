package com.fuelstation.controller;

import com.fuelstation.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationRestController {
    @Autowired private NotificationService notifications;
    @GetMapping
    public Map<String, Object> inbox(Authentication auth) {
        return Map.of("notifications", notifications.getNotificationsForCustomer(auth.getName()),
            "unreadCount", notifications.getUnreadCountForCustomer(auth.getName()));
    }
    @PatchMapping("/{id}/read")
    public Map<String, Object> read(@PathVariable Long id, Authentication auth) {
        notifications.markReadForUser(id, auth.getName());
        return Map.of("success", true);
    }
    @PostMapping("/read-all")
    public Map<String, Object> readAll(Authentication auth) {
        notifications.markAllReadForCustomer(auth.getName());
        return Map.of("success", true);
    }
    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id, Authentication auth) {
        notifications.deleteForUser(id, auth.getName());
        return Map.of("success", true);
    }
    public record Announcement(String title, String message, List<String> roles) {}
    @PostMapping("/announcements")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public Map<String, Object> announce(@RequestBody Announcement a) {
        if (a.title() == null || a.title().isBlank() || a.title().length() > 150 || a.message() == null || a.message().isBlank() || a.message().length() > 2000)
            throw new IllegalArgumentException("Enter a title (up to 150 characters) and message (up to 2000 characters)");
        List<String> roles = a.roles() == null || a.roles().isEmpty() ? List.of("ADMIN", "MANAGER", "CASHIER", "MECHANIC", "CUSTOMER") : a.roles();
        if (!List.of("ADMIN", "MANAGER", "CASHIER", "MECHANIC", "CUSTOMER").containsAll(roles)) throw new IllegalArgumentException("Invalid recipient role");
        notifications.notifyRoles(a.title().trim(), a.message().trim(), "Announcement", null, roles.toArray(String[]::new));
        return Map.of("success", true);
    }
}
