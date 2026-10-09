package com.fuelstation.controller;

import com.fuelstation.model.CustomerFeedback;
import com.fuelstation.service.WorkshopService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/customer/feedback")
public class CustomerFeedbackRestController {

    @Autowired
    private WorkshopService workshopService;

    @Autowired private com.fuelstation.repository.AppUserRepository accounts;
    @Autowired private com.fuelstation.service.BookingService bookings;
    @GetMapping("/mine") public Object mine(Authentication auth) { return workshopService.getAllFeedbacks().stream().filter(f -> auth.getName().equals(f.getCustomerUsername())).toList(); }
    @GetMapping("/jobs") public Object jobs(Authentication auth) { return bookings.getBookingsByCustomer(auth.getName()).stream().filter(b -> "Completed".equals(b.getStatus())).map(b -> workshopService.getJobCardByReferenceNumber(b.getReferenceNumber())).flatMap(java.util.Optional::stream).filter(j -> workshopService.getAllFeedbacks().stream().noneMatch(f -> j.getId().equals(f.getJobCardId()))).toList(); }

    private String getUsername(Authentication auth) {
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return auth.getName();
        }
        return null;
    }

    @PostMapping("/submit")
    public ResponseEntity<?> submitFeedback(@RequestBody CustomerFeedback feedback, Authentication auth) {
        String username = getUsername(auth);
        if (username == null) return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        
        feedback.setCustomerUsername(username);
        feedback.setCustomerName(accounts.findByUsername(username).orElseThrow().getFullName());

        // Auto flags
        if (feedback.getRating() != null && feedback.getRating() < 3) {
            feedback.setNeedsAttention(true);
        } else {
            feedback.setNeedsAttention(false);
        }
        
        feedback.setSubmissionDate(LocalDateTime.now());
        if(feedback.getCustomerName() == null || feedback.getCustomerName().isEmpty()){
            feedback.setCustomerName(username); // fallback to username if not provided
        }

        CustomerFeedback saved = workshopService.submitFeedback(feedback);
        return ResponseEntity.ok(Map.of("success", true, "feedback", saved));
    }
    
    // Allows public display of 4 and 5 star ratings.
    @GetMapping("/public")
    public ResponseEntity<?> getPublicFeedback() {
        List<CustomerFeedback> allFeedbacks = workshopService.getAllFeedbacks();
        
        List<CustomerFeedback> topFeedbacks = allFeedbacks.stream()
            .filter(f -> f.getRating() != null && f.getRating() >= 4)
            .sorted((a, b) -> b.getSubmissionDate().compareTo(a.getSubmissionDate()))
            .limit(20) // show top 20 latest good reviews
            .collect(Collectors.toList());
            
        return ResponseEntity.ok(topFeedbacks.stream().map(f -> {Map<String,Object> item=new java.util.HashMap<>();item.put("id",f.getId());item.put("customerName",f.getCustomerName());item.put("rating",f.getRating());item.put("comments",f.getComments());item.put("submissionDate",f.getSubmissionDate());return item;}).toList());
    }
}
