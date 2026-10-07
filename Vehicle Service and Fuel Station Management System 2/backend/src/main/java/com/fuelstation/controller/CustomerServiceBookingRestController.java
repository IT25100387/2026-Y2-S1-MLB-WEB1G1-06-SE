package com.fuelstation.controller;

import com.fuelstation.model.*;
import com.fuelstation.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/v1/customer/appointments")
public class CustomerServiceBookingRestController {
    @Autowired private com.fuelstation.service.SchedulingService scheduling;
    @Autowired private WorkshopService workshop;
    @Autowired private BillingService billing;
    @GetMapping("/track/{referenceNumber}/details") public Object details(@PathVariable String referenceNumber, Authentication auth) {
        ServiceBooking booking=bookingService.getBookingForCustomer(referenceNumber,auth.getName()).orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Booking is not yours"));
        var job=workshop.getJobCardByReferenceNumber(referenceNumber);
        Map<String,Object> result=new HashMap<>(); result.put("booking",booking); result.put("job",job.orElse(null)); result.put("usedParts",job.isPresent()?workshop.getUsedParts(job.get().getId()):List.of()); result.put("invoice",billing.getAllInvoices().stream().filter(i -> referenceNumber.equals(i.getReferenceNumber())).findFirst().orElse(null)); return result;
    }

    @GetMapping("/availability")
    public ResponseEntity<?> availability(@RequestParam String date, @RequestParam String serviceType, @RequestParam(required=false) String excludeReference, Authentication auth) {
        var service = serviceCatalogService.getServiceByName(serviceType).filter(com.fuelstation.model.ServiceCatalogItem::isActive).orElseThrow(() -> new IllegalArgumentException("Select an active service"));
        if (excludeReference != null) bookingService.getBookingForCustomer(excludeReference,auth.getName()).orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Booking is not yours"));
        java.time.LocalDate day = java.time.LocalDate.parse(date);
        var slots = com.fuelstation.service.SchedulingService.SLOTS.stream().map(s -> Map.of("timeSlot", s, "available", scheduling.hasCapacity(day, s, service.getEstimatedDuration(), excludeReference))).toList();
        return ResponseEntity.ok(Map.of("slots", slots));
    }

    @Autowired
    private BookingService bookingService;

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private ServiceCatalogService serviceCatalogService;
    
    @Autowired
    private OfferService offerService;

    private String getUsername(Authentication auth) {
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return auth.getName();
        }
        return null;
    }

    @GetMapping
    public ResponseEntity<?> getMyAppointments(Authentication auth) {
        String username = getUsername(auth);
        if (username == null) return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        
        List<ServiceBooking> bookings = bookingService.getBookingsByCustomer(username);
        for (ServiceBooking booking : bookings) { var inv=billing.getAllInvoices().stream().filter(i -> booking.getReferenceNumber().equals(i.getReferenceNumber())).findFirst(); booking.setInvoiceNumber(inv.map(Invoice::getInvoiceNumber).orElse(null)); booking.setAmountPaid(inv.map(Invoice::getAmountPaid).orElse(0.0)); booking.setEditable(Set.of("Pending","Approved").contains(booking.getStatus()) && workshop.getJobCardByReferenceNumber(booking.getReferenceNumber()).isEmpty()); booking.setCancellable("Pending".equals(booking.getStatus()) && inv.map(i -> com.fuelstation.util.Rules.amount(i.getAmountPaid())<=0 && com.fuelstation.util.Rules.amount(i.getRefundedAmount())<=0).orElse(true)); }
        // Customer rows expose the valid billing and cancellation state.
        return ResponseEntity.ok(bookings);
    }

    @PostMapping
    public ResponseEntity<?> createBooking(@RequestBody ServiceBooking bookingRequest, Authentication auth) {
        String username = getUsername(auth);
        if (username == null) return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));

        // Validation 1: Check date
        if (bookingRequest.getServiceDate() != null && bookingRequest.getServiceDate().isBefore(LocalDate.now())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Service date cannot be in the past"));
        }

        // Validation 2: Verify vehicle ownership
        List<Vehicle> myVehicles = vehicleService.getVehiclesByOwner(username);
        boolean owns = myVehicles.stream().anyMatch(v -> v.getLicensePlate().equals(bookingRequest.getLicensePlate()));
        if (!owns) {
            return ResponseEntity.status(403).body(Map.of("message", "Vehicle not found in your garage"));
        }

        // The creation service validates capacity for the actual package duration under a lock.

        // Apply Package details & Pricing
        Optional<ServiceCatalogItem> catalogOpt = serviceCatalogService.getServiceByName(bookingRequest.getServiceType());
        if (catalogOpt.isPresent()) {
            ServiceCatalogItem item = catalogOpt.get();
            bookingRequest.setEstimatedDuration(item.getEstimatedDuration());
            
            // Check offers
            List<Offer> activeOffers = offerService.getActiveOffersByType(OfferTargetType.SERVICE);
            double price = item.getEstimatedCost();
            for (Offer o : activeOffers) {
                if (o.getTargetId() != null && o.getTargetId().equals(item.getId())) {
                    price = price - (price * o.getDiscountPercentage() / 100.0);
                    break;
                }
            }
            bookingRequest.setEstimatedCost(price);
        } else {
            bookingRequest.setEstimatedCost(0.0);
        }

        bookingRequest.setCustomerUsername(username);
        bookingRequest.setStatus("Pending");
        bookingRequest.setTrackingStage("Vehicle Received");

        ServiceBooking saved = bookingService.createBooking(bookingRequest);
        return ResponseEntity.ok(Map.of("success", true, "message", "Booking confirmed", "booking", saved));
    }

    @PatchMapping("/{referenceNumber}/reschedule")
    public ResponseEntity<?> rescheduleBooking(@PathVariable String referenceNumber, @RequestBody Map<String, String> payload, Authentication auth) {
        String username = getUsername(auth);
        if (username == null) return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));

        LocalDate newDate = LocalDate.parse(payload.get("newDate"));
        String newSlot = payload.get("newSlot");

        try {
            ServiceBooking updated = bookingService.rescheduleBooking(referenceNumber, newDate, newSlot, username);
            return ResponseEntity.ok(Map.of("success", true, "message", "Rescheduled successfully", "booking", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PatchMapping("/{referenceNumber}/cancel")
    public ResponseEntity<?> cancelBooking(@PathVariable String referenceNumber, Authentication auth) {
        String username = getUsername(auth);
        if (username == null) return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));

        try {
            bookingService.cancelBookingForCustomer(referenceNumber, username);
            return ResponseEntity.ok(Map.of("success", true, "message", "Booking cancelled successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/track/{referenceNumber}")
    public ResponseEntity<?> trackBooking(@PathVariable String referenceNumber, Authentication auth) {
        String username = getUsername(auth);
        if (username == null) return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));

        Optional<ServiceBooking> bookingOpt = bookingService.getBookingForCustomer(referenceNumber, username);
        if (bookingOpt.isEmpty()) {
            return ResponseEntity.status(403).body(Map.of("message", "Access Denied"));
        }
        
        ServiceBooking booking = bookingOpt.get();
        return ResponseEntity.ok(booking);
    }
}
