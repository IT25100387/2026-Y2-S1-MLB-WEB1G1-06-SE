package com.fuelstation.service;

import com.fuelstation.model.AuditLog;
import java.util.List;

public interface AuditLogService {

    AuditLog record(String action, String username, String role, String details, String status);

    AuditLog record(String action, String username, String role, String details, String status, String ipAddress);

    List<AuditLog> getAllLogs();

    List<AuditLog> getRecentLogs();

    List<AuditLog> filterLogs(String action, String role, String status, String query);

    long getTotalCount();

    long getSuccessCount();

    long getSecurityEventsCount();
}
