package com.fuelstation.controller;

import com.fuelstation.service.BookingService;
import com.fuelstation.service.FuelService;
import com.fuelstation.service.StaffService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardRestController {

    @Autowired
    private FuelService fuelService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private StaffService staffService;
    @Autowired private com.fuelstation.service.BillingService billing;

    @GetMapping("/overview")
    public Map<String, Object> getDashboardOverview() {
        Map<String, Object> response = new HashMap<>();

        // Fetch same stats as original DashboardController.java
        response.put("totalRevenue", fuelService.getTotalRevenue()+billing.getAllPayments().stream().filter(p -> billing.getInvoiceById(p.getInvoiceId()).map(i -> !"FUEL".equals(i.getInvoiceType())).orElse(false)).mapToDouble(p -> com.fuelstation.util.Rules.amount(p.getAmount())-com.fuelstation.util.Rules.amount(p.getRefundedAmount())).sum());
        response.put("pendingBookings", bookingService.getPendingBookingsCount());
        response.put("completedBookings", bookingService.getCompletedBookingsCount());
        response.put("pendingLeaves", staffService.getPendingLeavesCount());
        response.put("pumpsCount", fuelService.getPumpsCount());
        response.put("inventories", fuelService.getAllInventories());
        
        // Optional: Sales preview

        return response;
    }
}
