package com.fuelstation.controller;

import com.fuelstation.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/mechanic")
public class MechanicRestController {
    @Autowired private WorkshopService workshop;
    @Autowired private BookingService bookings;
    @Autowired private BillingService billing;
    @Autowired private JobExpiryService expiry;
    @Autowired private java.time.Clock clock;
    @GetMapping("/workspace")
    public Map<String, Object> workspace(Authentication auth) {
        expiry.expireUnstartedJobs();
        List<Map<String, Object>> jobs = workshop.getAllJobCards().stream().map(job -> {
            Map<String, Object> row = new HashMap<>();
            var booking=bookings.getBookingByReferenceNumber(job.getReferenceNumber()).orElse(null);
            row.put("job", job); row.put("booking", booking); row.put("scheduledEnd", JobExpiryService.scheduledEnd(booking)); row.put("usedParts", workshop.getUsedParts(job.getId())); return row;
        }).toList();
        var parts=billing.getAllSpareParts().stream().map(part -> {
            Map<String,Object> row=new HashMap<>(); row.put("id",part.getId()); row.put("partName",part.getPartName());
            row.put("stockQuantity",part.getStockQuantity()); row.put("category",part.getCategory()); row.put("status",part.getStatus()); return row;
        }).toList();
        return Map.of("jobs", jobs, "parts", parts, "username", auth.getName(), "today", java.time.LocalDate.now(clock), "timeZone", clock.getZone().getId());
    }
    @PatchMapping("/jobs/{id}")
    public Map<String, Object> progress(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        workshop.getJobCardById(id).orElseThrow(() -> new IllegalArgumentException("Job not found"));
        expiry.expireJob(id);
        return Map.of("success", true, "job", workshop.updateJobCard(id, payload.get("status"), payload.get("mechanicNotes"), null));
    }
    public record PartRequest(Long partId, Integer quantity) {}
    @PostMapping("/jobs/{id}/parts")
    public Map<String, Object> part(@PathVariable Long id, @RequestBody PartRequest request, @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        return Map.of("success", true, "part", workshop.addUsedPart(id, request.partId(), request.quantity(), key));
    }
}
