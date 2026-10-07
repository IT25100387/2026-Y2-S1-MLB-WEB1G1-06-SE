package com.fuelstation.controller;

import com.fuelstation.model.AuditLog;
import com.fuelstation.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/audit-logs")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class AuditRestController {

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAuditLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q
    ) {
        List<AuditLog> logs = auditLogService.filterLogs(action, role, status, q);

        Map<String, Object> response = new HashMap<>();
        response.put("logs", logs);
        response.put("totalCount", auditLogService.getTotalCount());
        response.put("successCount", auditLogService.getSuccessCount());
        response.put("securityCount", auditLogService.getSecurityEventsCount());

        return ResponseEntity.ok(response);
    }
}
