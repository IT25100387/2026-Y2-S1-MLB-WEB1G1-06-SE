package com.fuelstation.controller;

import com.fuelstation.model.*;
import com.fuelstation.service.*;
import com.fuelstation.repository.InvoiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/pos")
public class PosRestController {
    @Autowired private BookingService bookings;
    @Autowired private BillingService billing;
    @Autowired private InvoiceRepository invoices;
    @Autowired private UserService users;
    @Autowired private VehicleService vehicles;
    @Autowired private OfferService offers;
    @GetMapping("/search")
    public Object search(@RequestParam String query) {
        return bookings.searchBookingsForPOS(query).stream().map(b -> {
            Map<String,Object> row = new HashMap<>();
            row.put("booking", b); row.put("id", b.getId()); row.put("referenceNumber", b.getReferenceNumber()); row.put("licensePlate", b.getLicensePlate()); row.put("customerName", b.getCustomerName()); row.put("customerPhone", b.getCustomerPhone()); row.put("customerUsername", b.getCustomerUsername()); row.put("customerType", bookings.deriveCustomerType(b)); row.put("serviceType", b.getServiceType()); row.put("serviceDate", b.getServiceDate()); row.put("timeSlot", b.getTimeSlot()); row.put("status", b.getStatus()); row.put("estimatedCost", b.getEstimatedCost()); row.put("notes", b.getNotes());
            var inv = invoices.findByReferenceNumber(b.getReferenceNumber()); row.put("hasInvoice", inv.isPresent()); row.put("invoice", inv.orElse(null)); return row;
        }).toList();
    }
    @GetMapping("/customers") public Object customers() { return users.getCustomersOnly().stream().map(u -> Map.of("username", u.getUsername(), "fullName", u.getFullName(), "phoneNumber", u.getPhoneNumber() == null ? "" : u.getPhoneNumber())).toList(); }
    @GetMapping("/vehicles") public Object vehicles() { return vehicles.getAllVehicles(); }
    public record ServiceCheckout(String referenceNumber, Double discount, String paymentMethod, Double amountWillingToPay, Double cashTendered) {}
    @PostMapping("/create-invoice") public Object service(@RequestBody ServiceCheckout input, @RequestHeader(value="Idempotency-Key", required=false) String key) {
        throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.GONE, "Use the single cashier POS checkout");
    }
    @GetMapping("/spare-parts") public Object parts(@RequestParam(required=false) String query) { String q = query == null ? "" : query.toLowerCase(); return billing.getAllSpareParts().stream().filter(p -> p.getStockQuantity() != null && p.getStockQuantity()>0 && !"Inactive".equalsIgnoreCase(p.getStatus())).filter(p -> p.getPartName().toLowerCase().contains(q) || p.getCategory()!=null && p.getCategory().toLowerCase().contains(q)).map(p -> { java.util.Map<String,Object> row=new java.util.HashMap<>(); row.put("id",p.getId()); row.put("partName",p.getPartName()); row.put("category",p.getCategory()); row.put("stockQuantity",p.getStockQuantity()); row.put("sellingPrice",p.getSellingPrice()); double discount=offers.getActiveOffersByType(OfferTargetType.SPARE_PART).stream().filter(o -> p.getId().equals(o.getTargetId())).findFirst().map(Offer::getDiscountPercentage).orElse(0.0); row.put("price", com.fuelstation.util.Rules.money(p.getSellingPrice()*(1-discount/100))); return row; }).toList(); }
    public record PartCheckout(Long partId, Integer quantity, String customerUsername, String licensePlate, String customerName, String paymentMethod, Double cashTendered) {}
    @PostMapping("/create-spare-part-invoice") public Object part(@RequestBody PartCheckout input, @RequestHeader(value="Idempotency-Key", required=false) String key) { throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.GONE, "Use the single cashier POS checkout"); }
}
