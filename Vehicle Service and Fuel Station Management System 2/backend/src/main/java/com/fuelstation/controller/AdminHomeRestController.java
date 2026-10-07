package com.fuelstation.controller;

import com.fuelstation.repository.AuditLogRepository;
import com.fuelstation.service.*;
import com.fuelstation.util.Rules;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api/dashboard/admin-overview")
@PreAuthorize("hasRole('ADMIN')")
public class AdminHomeRestController {
    private final UserService users;
    private final BillingService billing;
    private final FuelService fuel;
    private final BookingService bookings;
    private final StaffService staff;
    private final SupportTicketService support;
    private final AuditLogRepository audit;
    private final Clock clock;

    public AdminHomeRestController(UserService users, BillingService billing, FuelService fuel, BookingService bookings,
                                   StaffService staff, SupportTicketService support, AuditLogRepository audit, Clock clock) {
        this.users=users;this.billing=billing;this.fuel=fuel;this.bookings=bookings;
        this.staff=staff;this.support=support;this.audit=audit;this.clock=clock;
    }
    @GetMapping
    @Transactional(readOnly=true)
    public Map<String,Object> overview() {
        var invoices=new HashMap<Long,String>();
        billing.getAllInvoices().forEach(invoice->invoices.put(invoice.getId(),invoice.getInvoiceType()));
        double collection=fuel.getTotalRevenue()+billing.getAllPayments().stream()
            .filter(payment->invoices.containsKey(payment.getInvoiceId())&&!"FUEL".equals(invoices.get(payment.getInvoiceId())))
            .mapToDouble(payment->Rules.amount(payment.getAmount())-Rules.amount(payment.getRefundedAmount())).sum();
        long lowFuel=fuel.getAllInventories().stream().filter(tank->tank.getCurrentStockLitres()!=null&&tank.getMinStockWarning()!=null
            &&tank.getCurrentStockLitres()<=tank.getMinStockWarning()).count();
        long lowParts=billing.getAllSpareParts().stream().filter(part->!Set.of("Inactive","Deactivated").contains(part.getStatus()==null?"":part.getStatus()))
            .filter(part->part.getStockQuantity()!=null&&part.getStockQuantity()<=(part.getMinStockWarning()==null?0:part.getMinStockWarning())).count();
        return Map.of(
            "activeAccounts",users.getAllUsers().stream().filter(user->"Active".equalsIgnoreCase(user.getStatus())).count(),
            "netCollection",Rules.money(collection),"pendingBookings",bookings.getPendingBookingsCount(),
            "pendingLeave",staff.getPendingLeavesCount(),"lowFuelCount",lowFuel,"lowPartsCount",lowParts,
            "openTickets",support.getOpenTicketsCount(),
            "recentAudit",audit.findAll(PageRequest.of(0,5,Sort.by(Sort.Direction.DESC,"timestamp","id"))).getContent(),
            "stationDate",LocalDate.now(clock),"refreshedAt",OffsetDateTime.now(clock));
    }
}
