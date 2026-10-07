package com.fuelstation.controller;

import com.fuelstation.model.CustomerFeedback;
import com.fuelstation.model.SupportTicket;
import com.fuelstation.service.SupportTicketService;
import com.fuelstation.service.WorkshopService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class SupportRestController {

    @Autowired
    private SupportTicketService supportTicketService;

    @Autowired
    private WorkshopService workshopService;

    // ==========================================
    // 1. SUPPORT TICKETS (Helpdesk)
    // ==========================================

    @GetMapping("/support/admin")
    public ResponseEntity<Map<String, Object>> getAdminSupportTickets(@RequestParam(required = false, defaultValue = "ALL") String status) {
        List<SupportTicket> allTickets = supportTicketService.getAllTickets();
        
        List<SupportTicket> displayed = allTickets;
        if (!"ALL".equalsIgnoreCase(status) && !status.isBlank()) {
            displayed = allTickets.stream()
                    .filter(t -> status.equalsIgnoreCase(t.getStatus()))
                    .collect(Collectors.toList());
        }

        long openCount = allTickets.stream().filter(t -> "OPEN".equalsIgnoreCase(t.getStatus())).count();
        long inProgressCount = allTickets.stream().filter(t -> "IN_PROGRESS".equalsIgnoreCase(t.getStatus())).count();
        long resolvedCount = allTickets.stream().filter(t -> "RESOLVED".equalsIgnoreCase(t.getStatus()) || "CLOSED".equalsIgnoreCase(t.getStatus())).count();

        Map<String, Object> response = new HashMap<>();
        response.put("tickets", displayed);
        response.put("totalTickets", allTickets.size());
        response.put("openCount", openCount);
        response.put("inProgressCount", inProgressCount);
        response.put("resolvedCount", resolvedCount);
        
        return ResponseEntity.ok(response);
    }

    @PutMapping("/support/admin/reply/{id}")
    public ResponseEntity<?> replyToTicket(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload
    ) {
        String adminReply = payload.get("adminReply");
        String status = payload.getOrDefault("status", "RESOLVED");

        supportTicketService.resolveTicket(id, adminReply, status);
        return ResponseEntity.ok(Map.of("success", true, "message", "Support ticket resolution and reply logged."));
    }

    // ==========================================
    // 2. CUSTOMER FEEDBACK
    // ==========================================

    @GetMapping("/feedback")
    public ResponseEntity<List<CustomerFeedback>> getAllFeedbacks() {
        return ResponseEntity.ok(workshopService.getAllFeedbacks());
    }

    @PostMapping("/feedback")
    public ResponseEntity<?> submitFeedback(@RequestBody CustomerFeedback feedback) {
        CustomerFeedback saved = workshopService.submitFeedback(feedback);
        return ResponseEntity.ok(Map.of("success", true, "message", "Feedback submitted successfully", "feedback", saved));
    }
}
