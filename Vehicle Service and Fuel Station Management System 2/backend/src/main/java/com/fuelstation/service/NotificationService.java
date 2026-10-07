package com.fuelstation.service;

import com.fuelstation.model.Notification;
import java.util.List;

public interface NotificationService {
    void createNotification(String title, String message, String type);
    List<Notification> getAllNotifications();
    List<Notification> getTopRecentNotifications(int limit);
    long getUnreadCount();
    void markAsRead(Long id);
    void deleteNotification(Long id);

    // Customer Notification Extensions
    void createCustomerNotification(String recipientUsername, String title, String message, String type, String actionUrl, String icon);
    List<Notification> getNotificationsForCustomer(String username);
    long getUnreadCountForCustomer(String username);
    void markAllReadForCustomer(String username);
    void notifyRoles(String title, String message, String type, String actionUrl, String... roles);
    void bookingChanged(com.fuelstation.model.ServiceBooking booking, String title);
    void jobChanged(com.fuelstation.model.JobCard job, String title);
    void invoiceChanged(com.fuelstation.model.Invoice invoice, String title);
    void markReadForUser(Long id, String username);
    void deleteForUser(Long id, String username);
}
