package com.fuelstation.controller;

import com.fuelstation.model.Invoice;
import com.fuelstation.model.ServiceBooking;
import com.fuelstation.model.Vehicle;
import com.fuelstation.service.BookingService;
import com.fuelstation.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/workshop/bookings")
public class BookingRestController {

    @Autowired
    private BookingService bookingService;
    
    @Autowired
    private VehicleService vehicleService;
    @Autowired private com.fuelstation.service.SchedulingService scheduling;
    @Autowired private com.fuelstation.service.ServiceCatalogService catalog;
    @Autowired private com.fuelstation.service.BillingService billing;
    @GetMapping("/availability")
    public Object slots(@RequestParam String date, @RequestParam String serviceType, @RequestParam(required=false) String excludeReference) {
        var service = catalog.getServiceByName(serviceType).orElseThrow(() -> new IllegalArgumentException("Service not found"));
        var day = java.time.LocalDate.parse(date);
        return scheduling.getAvailabilityInfo(day, service.getEstimatedDuration(), excludeReference);
    }

    private boolean isCustomer(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return false;
        for (GrantedAuthority ga : auth.getAuthorities()) {
            if ("ROLE_CUSTOMER".equalsIgnoreCase(ga.getAuthority()) || "Customer".equalsIgnoreCase(ga.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    @GetMapping
    public Map<String, Object> getAllBookings(Authentication auth) {
        Map<String, Object> response = new HashMap<>();
        
        if (isCustomer(auth)) {
            response.put("error", "Access Denied");
            return response;
        }

        try {
            List<ServiceBooking> allBookings = bookingService.getAllBookings();
            List<ServiceBooking> pendingBookings = bookingService.getPendingBookings();
            Set<Long> pendingIds = (pendingBookings != null ? pendingBookings : Collections.<ServiceBooking>emptyList()).stream()
                .map(ServiceBooking::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

            List<Invoice> allInvoices = billing.getAllInvoices();
            Map<String, Invoice> invoiceByRef = (allInvoices != null ? allInvoices : Collections.<Invoice>emptyList()).stream()
                .filter(i -> i.getReferenceNumber() != null && !i.getReferenceNumber().isBlank())
                .collect(Collectors.toMap(Invoice::getReferenceNumber, i -> i, (a, b) -> a));

            List<Map<String, Object>> bookingList = new ArrayList<>();
            
            for (ServiceBooking b : (allBookings != null ? allBookings : Collections.<ServiceBooking>emptyList())) {
                if (b == null) continue;
                Map<String, Object> map = new HashMap<>();
                map.put("id", b.getId());
                map.put("editable", b.getId() != null && pendingIds.contains(b.getId()));
                
                Invoice invoice = (b.getReferenceNumber() != null && !b.getReferenceNumber().isBlank()) ? invoiceByRef.get(b.getReferenceNumber()) : null;
                map.put("invoiceId", invoice == null ? null : invoice.getId());
                map.put("invoiceNumber", invoice == null ? null : invoice.getInvoiceNumber());
                map.put("cancellable", "Pending".equals(b.getStatus()) && (invoice == null || 
                    (com.fuelstation.util.Rules.amount(invoice.getAmountPaid()) == 0 && com.fuelstation.util.Rules.amount(invoice.getRefundedAmount()) == 0)));
                map.put("referenceNumber", b.getReferenceNumber() != null ? b.getReferenceNumber() : "");
                map.put("customerName", b.getCustomerName());
                map.put("customerPhone", b.getCustomerPhone());
                map.put("licensePlate", b.getLicensePlate());
                map.put("serviceDate", b.getServiceDate() != null ? b.getServiceDate().toString() : "");
                map.put("timeSlot", b.getTimeSlot());
                map.put("timeRange", b.getTimeRange());
                map.put("estimatedDuration", b.getEstimatedDuration());
                map.put("serviceType", b.getServiceType());
                map.put("status", b.getStatus());
                map.put("notes", b.getNotes());
                map.put("estimatedCost", b.getEstimatedCost());
                map.put("customerType", bookingService.deriveCustomerType(b));
                bookingList.add(map);
            }
            
            response.put("bookings", bookingList);
            
            List<Vehicle> allVehicles = vehicleService.getAllVehicles();
            response.put("vehicles", allVehicles != null ? allVehicles : List.of());
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(BookingRestController.class).error("Error loading workshop bookings", e);
            response.put("bookings", List.of());
            response.put("vehicles", List.of());
        }
        
        return response;
    }

    @PostMapping("/add")
    public Map<String, Object> addBooking(@RequestBody ServiceBooking booking, Authentication auth) {
        Map<String, Object> response = new HashMap<>();
        if (isCustomer(auth)) {
            response.put("success", false);
            response.put("error", "Access Denied");
            return response;
        }
        ServiceBooking saved = bookingService.createBooking(booking);
        response.put("success", true);
        response.put("referenceNumber", saved.getReferenceNumber());
        return response;
    }

    @PostMapping("/status/{referenceNumber}")
    public Map<String, Object> updateBookingStatus(@PathVariable String referenceNumber, @RequestBody Map<String, String> payload, Authentication auth) {
        Map<String, Object> response = new HashMap<>();
        if (isCustomer(auth)) {
            response.put("success", false);
            return response;
        }
        String status = payload.get("status");
        bookingService.updateBookingStatus(referenceNumber, status);
        response.put("success", true);
        return response;
    }

    @PostMapping("/admin-update/{referenceNumber}")
    public Map<String, Object> adminUpdateBooking(@PathVariable String referenceNumber, @RequestBody Map<String, String> payload, Authentication auth) {
        Map<String, Object> response = new HashMap<>();
        if (isCustomer(auth)) {
            response.put("success", false);
            return response;
        }
        String serviceDate = payload.get("serviceDate");
        String timeSlot = payload.get("timeSlot");
        String serviceType = payload.get("serviceType");
        
        bookingService.updateBookingAdmin(referenceNumber, java.time.LocalDate.parse(serviceDate), timeSlot, serviceType, payload.get("notes"));
        response.put("success", true);
        return response;
    }

    @PostMapping("/delete/{referenceNumber}")
    public Map<String, Object> deleteBooking(@PathVariable String referenceNumber, Authentication auth) {
        Map<String, Object> response = new HashMap<>();
        if (isCustomer(auth)) {
            response.put("success", false);
            return response;
        }
        bookingService.deleteBooking(referenceNumber);
        response.put("success", true);
        return response;
    }
}
