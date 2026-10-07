package com.fuelstation.controller;

import com.fuelstation.model.ServiceBooking;
import com.fuelstation.model.Vehicle;
import com.fuelstation.service.BookingService;
import com.fuelstation.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        return Map.of("slots", com.fuelstation.service.SchedulingService.SLOTS.stream().map(slot -> Map.of("timeSlot",slot,"available",scheduling.hasCapacity(day,slot,service.getEstimatedDuration(),excludeReference))).toList());
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

        List<ServiceBooking> allBookings = bookingService.getAllBookings();
        List<Map<String, Object>> bookingList = new ArrayList<>();
        
        for (ServiceBooking b : allBookings) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", b.getId());
            map.put("editable",bookingService.getPendingBookings().stream().anyMatch(p -> p.getId().equals(b.getId())));
            var invoice=billing.getAllInvoices().stream().filter(i -> b.getReferenceNumber().equals(i.getReferenceNumber())).findFirst().orElse(null);
            map.put("invoiceId",invoice==null?null:invoice.getId());
            map.put("invoiceNumber",invoice==null?null:invoice.getInvoiceNumber());
            map.put("cancellable","Pending".equals(b.getStatus()) && (invoice==null || com.fuelstation.util.Rules.amount(invoice.getAmountPaid())==0 && com.fuelstation.util.Rules.amount(invoice.getRefundedAmount())==0));
            map.put("referenceNumber", b.getReferenceNumber());
            map.put("customerName", b.getCustomerName());
            map.put("customerPhone", b.getCustomerPhone());
            map.put("licensePlate", b.getLicensePlate());
            map.put("serviceDate", b.getServiceDate() != null ? b.getServiceDate().toString() : "");
            map.put("timeSlot", b.getTimeSlot());
            map.put("serviceType", b.getServiceType());
            map.put("status", b.getStatus());
            map.put("notes", b.getNotes());
            map.put("estimatedCost", b.getEstimatedCost());
            map.put("customerType", bookingService.deriveCustomerType(b));
            bookingList.add(map);
        }
        
        response.put("bookings", bookingList);
        
        List<Vehicle> allVehicles = vehicleService.getAllVehicles();
        response.put("vehicles", allVehicles);
        
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
