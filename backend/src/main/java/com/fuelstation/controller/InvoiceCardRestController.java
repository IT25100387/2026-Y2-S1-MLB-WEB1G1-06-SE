package com.fuelstation.controller;

import com.fuelstation.service.CashierCardService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;

/** All status/cancel operations enforce checkout ownership in the shared service. */
@RestController @RequestMapping("/api/payments/card")
public class InvoiceCardRestController {
    private final CashierCardService cards;
    public InvoiceCardRestController(CashierCardService cards){this.cards=cards;}
    @GetMapping("/configuration") public Object configuration(){return cards.configuration();}
    @PostMapping("/start") public Object start(@RequestBody CashierCardService.InvoiceRequest request,@RequestHeader("Idempotency-Key") String key,Authentication auth){return cards.startInvoice(request,key,auth.getName(),auth.getAuthorities().stream().anyMatch(a->"ROLE_CUSTOMER".equals(a.getAuthority())));}
    @PostMapping("/store/start") @PreAuthorize("hasRole('CUSTOMER')")
    public Object store(@RequestBody CashierCardService.StoreRequest request,@RequestHeader("Idempotency-Key") String key,Authentication auth){return cards.startStore(request,key,auth.getName());}
    @GetMapping("/{id}") public Object status(@PathVariable String id,Authentication auth){return cards.status(id,auth.getName());}
    @PostMapping("/{id}/cancel-unprocessed") public Object cancel(@PathVariable String id,Authentication auth){return cards.cancelUnprocessed(id,auth.getName());}
    @PostMapping("/{id}/demo-complete") public Object demo(@PathVariable String id,Authentication auth){return cards.completeDemo(id,auth.getName());}
}
