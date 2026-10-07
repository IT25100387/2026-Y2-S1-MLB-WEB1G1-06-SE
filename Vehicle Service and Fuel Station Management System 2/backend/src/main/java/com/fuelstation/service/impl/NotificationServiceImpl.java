package com.fuelstation.service.impl;

import com.fuelstation.model.*;
import com.fuelstation.repository.*;
import com.fuelstation.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private AppUserRepository userRepository;

    @Override
    public void createNotification(String title, String message, String type) {
        notifyRoles(title, message, type, null, "ADMIN", "MANAGER");
    }
    @Override
    public void notifyRoles(String title, String message, String type, String actionUrl, String... roles) {
        Set<String> targets = new HashSet<>(Arrays.asList(roles));
        for (AppUser user : userRepository.findAll()) {
            String role = user.getRole() == null ? "" : user.getRole().toUpperCase(Locale.ROOT).replace("ROLE_", "");
            if (targets.contains(role) && !"Deactivated".equalsIgnoreCase(user.getStatus()))
                createCustomerNotification(user.getUsername(), title, message, type, actionUrl, "fa-bell");
        }
    }
    @Override
    public void createCustomerNotification(String username, String title, String message, String type, String actionUrl, String icon) {
        if (username == null || username.isBlank()) return;
        Notification n = new Notification();
        n.setRecipientUsername(username.trim());
        n.setTitle(title);
        n.setMessage(message);
        n.setType(type == null ? "General" : type);
        n.setActionUrl(actionUrl);
        n.setIcon(icon == null ? "fa-bell" : icon);
        n.setStatus("Unread");
        n.setCreatedDate(LocalDateTime.now());
        notificationRepository.save(n);
    }
    @Override
    public void bookingChanged(ServiceBooking b, String title) {
        String message = b.getReferenceNumber() + " | " + b.getLicensePlate() + " | " + b.getStatus();
        notifyRoles(title, message, "Booking", "/dashboard/bookings", "MANAGER", "CASHIER");
        createCustomerNotification(b.getCustomerUsername(), title, message, "Booking", "/customer/appointments", "fa-calendar-check");
    }
    @Override
    public void jobChanged(JobCard job, String title) {
        String message = job.getReferenceNumber() + " | " + job.getLicensePlate() + " | " + job.getStatus();
        notifyRoles(title, message, "Service", "/dashboard/workshop/job-cards", "MANAGER");
        notifyRoles(title, message, "Service", "/dashboard/bookings", "CASHIER");
        createCustomerNotification(job.getMechanicUsername(), title, message, "Service", "/dashboard/mechanic-workspace", "fa-wrench");
    }
    @Override
    public void invoiceChanged(Invoice invoice, String title) {
        String message = invoice.getInvoiceNumber() + " | " + (invoice.getCustomerName() == null ? "Customer" : invoice.getCustomerName()) + " | " + invoice.getCalculatedStatus();
        notifyRoles(title, message, "Billing", "/dashboard/invoices", "MANAGER", "CASHIER");
        createCustomerNotification(invoice.getCustomerUsername(), title, message, "Billing", "/customer/invoices/" + invoice.getInvoiceNumber(), "fa-file-invoice");
    }
    private String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) throw new AccessDeniedException("Sign in to access notifications");
        return auth.getName();
    }
    @Override @Transactional(readOnly = true)
    public List<Notification> getAllNotifications() { return getNotificationsForCustomer(currentUsername()); }
    @Override @Transactional(readOnly = true)
    public List<Notification> getTopRecentNotifications(int limit) { return getAllNotifications().stream().limit(Math.max(0, limit)).toList(); }
    @Override @Transactional(readOnly = true)
    public long getUnreadCount() { return getUnreadCountForCustomer(currentUsername()); }
    @Override @Transactional(readOnly = true)
    public List<Notification> getNotificationsForCustomer(String username) {
        return username == null || username.isBlank() ? List.of() : notificationRepository.findByRecipientUsernameOrderByCreatedDateDesc(username.trim());
    }
    @Override @Transactional(readOnly = true)
    public long getUnreadCountForCustomer(String username) {
        return username == null || username.isBlank() ? 0 : notificationRepository.countByRecipientUsernameAndStatus(username.trim(), "Unread");
    }
    @Override public void markAsRead(Long id) { markReadForUser(id, currentUsername()); }
    @Override public void deleteNotification(Long id) { deleteForUser(id, currentUsername()); }
    @Override
    public void markReadForUser(Long id, String username) {
        Notification n = notificationRepository.findByIdAndRecipientUsername(id, username)
            .orElseThrow(() -> new AccessDeniedException("Notification is not in your inbox"));
        n.setStatus("Read");
        notificationRepository.save(n);
    }
    @Override
    public void deleteForUser(Long id, String username) {
        Notification n = notificationRepository.findByIdAndRecipientUsername(id, username)
            .orElseThrow(() -> new AccessDeniedException("Notification is not in your inbox"));
        notificationRepository.delete(n);
    }
    @Override
    public void markAllReadForCustomer(String username) {
        List<Notification> rows = getNotificationsForCustomer(username);
        rows.forEach(n -> n.setStatus("Read"));
        notificationRepository.saveAll(rows);
    }
}
