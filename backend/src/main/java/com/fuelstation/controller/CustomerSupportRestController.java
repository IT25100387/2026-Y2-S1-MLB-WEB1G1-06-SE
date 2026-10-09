package com.fuelstation.controller;

import com.fuelstation.model.SupportTicket;
import com.fuelstation.service.SupportTicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/customer/support")
public class CustomerSupportRestController {

    @Autowired
    private SupportTicketService supportTicketService;

    private String getUsername(Authentication auth) {
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return auth.getName();
        }
        return null;
    }

    @GetMapping
    public ResponseEntity<?> getCustomerTickets(Authentication auth) {
        String username = getUsername(auth);
        if (username == null) return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));

        List<SupportTicket> tickets = supportTicketService.getTicketsForCustomer(username);
        long openCount = tickets.stream()
                .filter(t -> "OPEN".equals(t.getStatus()) || "IN_PROGRESS".equals(t.getStatus()))
                .count();

        return ResponseEntity.ok(Map.of(
                "tickets", tickets,
                "stats", Map.of("openCount", openCount)
        ));
    }

    @PostMapping
    public ResponseEntity<?> createTicket(@RequestBody SupportTicket ticket, Authentication auth) {
        String username = getUsername(auth);
        if (username == null) return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));

        SupportTicket created = supportTicketService.createTicket(ticket, username);
        return ResponseEntity.ok(Map.of("success", true, "ticket", created));
    }
}
