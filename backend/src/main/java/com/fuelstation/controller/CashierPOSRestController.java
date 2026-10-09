package com.fuelstation.controller;

import com.fuelstation.service.CashierPOSService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pos")
public class CashierPOSRestController {
    @Autowired private CashierPOSService pos;
    @GetMapping("/products") public Object products(){return pos.products();}
    @GetMapping("/catalog") public Object catalog(){return pos.services();}
    @GetMapping("/reference/{reference}") public Object reference(@PathVariable String reference){return pos.quote(new CashierPOSService.Checkout(reference,java.util.List.of(),null,null,null,null,null));}
    @PostMapping("/quote") public Object quote(@RequestBody CashierPOSService.Checkout input){return pos.quote(input);}
    @PostMapping("/checkout") public Object checkout(@RequestBody CashierPOSService.Checkout input,@RequestHeader("Idempotency-Key") String key,Authentication auth){return pos.checkout(input,key,auth.getName());}
    @GetMapping("/invoices/today") public Object today(){return pos.today();}
    @GetMapping("/checkouts/{key}") public Object status(@PathVariable String key,Authentication auth){return pos.checkoutStatus(key,auth.getName());}
}
