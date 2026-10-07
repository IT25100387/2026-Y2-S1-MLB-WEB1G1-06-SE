package com.fuelstation.service.impl;

import com.fuelstation.model.AuditLog;
import com.fuelstation.repository.AuditLogRepository;
import com.fuelstation.service.AuditLogService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Override
    @Transactional
    public AuditLog record(String action, String username, String role, String details, String status) {
        return record(action, username, role, details, status, "127.0.0.1");
    }

    @Override
    @Transactional
    public AuditLog record(String action, String username, String role, String details, String status, String ipAddress) {
        AuditLog log = AuditLog.builder()
                .action(action != null ? action.trim().toUpperCase() : "GENERAL")
                .username(username != null ? username.trim() : "system")
                .userRole(role != null ? role.trim() : "System")
                .details(details != null ? details.trim() : "")
                .status(status != null ? status.trim().toUpperCase() : "SUCCESS")
                .ipAddress(ipAddress != null ? ipAddress.trim() : "127.0.0.1")
                .timestamp(LocalDateTime.now())
                .build();
        return auditLogRepository.save(log);
    }

    @Override
    public List<AuditLog> getAllLogs() {
        return auditLogRepository.findAllByOrderByTimestampDesc();
    }

    @Override
    public List<AuditLog> getRecentLogs() {
        return auditLogRepository.findTop100ByOrderByTimestampDesc();
    }

    @Override
    public List<AuditLog> filterLogs(String action, String role, String status, String query) {
        List<AuditLog> logs = auditLogRepository.findTop100ByOrderByTimestampDesc();
        return logs.stream()
                .filter(l -> action == null || action.isBlank() || "ALL".equalsIgnoreCase(action) || action.equalsIgnoreCase(l.getAction()))
                .filter(l -> role == null || role.isBlank() || "ALL".equalsIgnoreCase(role) || role.equalsIgnoreCase(l.getUserRole()))
                .filter(l -> status == null || status.isBlank() || "ALL".equalsIgnoreCase(status) || status.equalsIgnoreCase(l.getStatus()))
                .filter(l -> query == null || query.isBlank() ||
                        (l.getUsername() != null && l.getUsername().toLowerCase().contains(query.toLowerCase())) ||
                        (l.getDetails() != null && l.getDetails().toLowerCase().contains(query.toLowerCase())) ||
                        (l.getAction() != null && l.getAction().toLowerCase().contains(query.toLowerCase())))
                .toList();
    }

    @Override
    public long getTotalCount() {
        return auditLogRepository.count();
    }

    @Override
    public long getSuccessCount() {
        return auditLogRepository.countByStatus("SUCCESS");
    }

    @Override
    public long getSecurityEventsCount() {
        List<AuditLog> all = auditLogRepository.findAll();
        return all.stream()
                .filter(l -> l.getAction() != null && (
                        l.getAction().contains("AUTH") ||
                        l.getAction().contains("PASSWORD") ||
                        l.getAction().contains("SECURITY") ||
                        l.getAction().contains("LOGIN")
                ))
                .count();
    }

}
