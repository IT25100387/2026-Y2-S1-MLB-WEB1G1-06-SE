package com.fuelstation.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.util.Map;

/** Keeps old page links usable after the server-rendered frontend is removed. */
@RestController
public class FrontendPageRedirectController {
    private final String frontendUrl;

    public FrontendPageRedirectController(@Value("${app.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    private static final Map<String, String> PAGES = Map.ofEntries(
        Map.entry("/", "/"),
        Map.entry("/dashboard", "/dashboard"),
        Map.entry("/mechanic/dashboard", "/dashboard/mechanic-workspace"),
        Map.entry("/customer/dashboard", "/customer"),
        Map.entry("/login", "/login"),
        Map.entry("/signup", "/signup"),
        Map.entry("/forgot-password", "/forgot-password"),
        Map.entry("/reset-password", "/reset-password"),
        Map.entry("/admin/audit-logs", "/dashboard/audit-logs"),
        Map.entry("/audit-logs", "/dashboard/audit-logs"),
        Map.entry("/admin/setting-services", "/dashboard/offers/service-catalog"),
        Map.entry("/admin/setting-service-offers", "/dashboard/offers/services"),
        Map.entry("/admin/setting-spare-part-offers", "/dashboard/offers/spare-parts"),
        Map.entry("/invoices", "/dashboard/invoices"),
        Map.entry("/spare-part-invoices", "/dashboard/invoices"),
        Map.entry("/payments", "/dashboard/payments"),
        Map.entry("/suppliers", "/dashboard/inventory/suppliers"),
        Map.entry("/spare-parts", "/dashboard/inventory/parts"),
        Map.entry("/customer/invoices", "/customer/invoices"),
        Map.entry("/customer/invoices/{invoiceNumber}", "/customer/invoices/{invoiceNumber}"),
        Map.entry("/customer/payments", "/customer/payments"),
        Map.entry("/customer/fuel-prices", "/customer/fuel"),
        Map.entry("/customer/prices", "/customer/fuel"),
        Map.entry("/customer/vehicles", "/customer/vehicles"),
        Map.entry("/customer/garage", "/customer/vehicles"),
        Map.entry("/customer/vehicles/view/{id}", "/customer/vehicles/{id}"),
        Map.entry("/customer/garage/view/{id}", "/customer/vehicles/{id}"),
        Map.entry("/customer/vehicles/edit/{id}", "/customer/vehicles/{id}"),
        Map.entry("/customer/garage/edit/{id}", "/customer/vehicles/{id}"),
        Map.entry("/customer/notifications", "/customer/notifications"),
        Map.entry("/customer/offers", "/customer/offers"),
        Map.entry("/customer/profile", "/customer/profile"),
        Map.entry("/customer/change-password", "/customer/profile/security"),
        Map.entry("/customer/settings", "/customer/settings"),
        Map.entry("/customer/services", "/customer/catalog"),
        Map.entry("/customer/book", "/customer/book"),
        Map.entry("/customer/appointments", "/customer/appointments"),
        Map.entry("/customer/track/{referenceNumber}", "/customer/track/{referenceNumber}"),
        Map.entry("/customer/service-history", "/customer/service-history"),
        Map.entry("/customer/spare-parts", "/customer/store"),
        Map.entry("/customer/support", "/customer/support"),
        Map.entry("/admin/support", "/dashboard/support/tickets"),
        Map.entry("/settings", "/dashboard/settings"),
        Map.entry("/inventory", "/dashboard/inventory/fuel"),
        Map.entry("/inventory-update", "/dashboard/inventory/fuel"),
        Map.entry("/sales", "/dashboard/fuel/sales"),
        Map.entry("/prices", "/dashboard/fuel/prices"),
        Map.entry("/notifications", "/dashboard/notifications"),
        Map.entry("/staff", "/dashboard/hr/staff"),
        Map.entry("/shifts", "/dashboard/hr/shifts"),
        Map.entry("/leave", "/dashboard/hr/leave"),
        Map.entry("/users", "/dashboard/users"),
        Map.entry("/users/new", "/dashboard/users"),
        Map.entry("/users/create", "/dashboard/users"),
        Map.entry("/users/{id}", "/dashboard/users"),
        Map.entry("/users/view/{id}", "/dashboard/users"),
        Map.entry("/users/edit/{id}", "/dashboard/users"),
        Map.entry("/customers", "/dashboard/customers"),
        Map.entry("/vehicles", "/dashboard/vehicles"),
        Map.entry("/vehicles/edit/{id}", "/dashboard/vehicles"),
        Map.entry("/bookings", "/dashboard/bookings"),
        Map.entry("/service-history", "/dashboard/service-history"),
        Map.entry("/workshop", "/dashboard/workshop/job-cards"),
        Map.entry("/workshop/bays", "/dashboard/workshop/bays"),
        Map.entry("/workshop/mechanics", "/dashboard/workshop/mechanics"),
        Map.entry("/feedback", "/dashboard/support/feedback"),
        Map.entry("/customer/feedback", "/customer/feedback")
    );

    @GetMapping({
        "/", "/dashboard", "/mechanic/dashboard", "/customer/dashboard",
        "/login", "/signup", "/forgot-password", "/reset-password",
        "/admin/audit-logs", "/audit-logs", "/admin/setting-services",
        "/admin/setting-service-offers", "/admin/setting-spare-part-offers",
        "/invoices", "/spare-part-invoices", "/payments", "/suppliers", "/spare-parts",
        "/customer/invoices", "/customer/invoices/{invoiceNumber}", "/customer/payments",
        "/customer/fuel-prices", "/customer/prices", "/customer/vehicles", "/customer/garage",
        "/customer/vehicles/view/{id}", "/customer/garage/view/{id}",
        "/customer/vehicles/edit/{id}", "/customer/garage/edit/{id}",
        "/customer/notifications", "/customer/offers", "/customer/profile",
        "/customer/change-password", "/customer/settings", "/customer/services",
        "/customer/book", "/customer/appointments", "/customer/track/{referenceNumber}",
        "/customer/service-history", "/customer/spare-parts", "/customer/support", "/admin/support",
        "/settings", "/inventory", "/inventory-update", "/sales", "/prices", "/notifications",
        "/staff", "/shifts", "/leave", "/users", "/users/new", "/users/create", "/users/{id}",
        "/users/view/{id}", "/users/edit/{id}", "/customers", "/vehicles", "/vehicles/edit/{id}",
        "/bookings", "/service-history", "/workshop", "/workshop/bays", "/workshop/mechanics",
        "/feedback", "/customer/feedback"
    })
    @SuppressWarnings("unchecked")
    public ResponseEntity<Void> page(HttpServletRequest request) {
        String pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE).toString();
        Map<String, ?> variables = (Map<String, ?>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        var page = UriComponentsBuilder.fromUriString(frontendUrl).path(PAGES.get(pattern))
            .encode().buildAndExpand(variables == null ? Map.of() : variables).toUri();
        var target = UriComponentsBuilder.fromUri(page);
        request.getParameterMap().forEach((key, values) -> {
            // Encode each value strictly so recovery-token '+' characters remain intact.
            for (String value : values) target.queryParam(
                UriUtils.encode(key, java.nio.charset.StandardCharsets.UTF_8),
                UriUtils.encode(value, java.nio.charset.StandardCharsets.UTF_8));
        });
        return ResponseEntity.status(HttpStatus.FOUND)
            .location(target.build(true).toUri())
            .build();
    }
}
