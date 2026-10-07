package com.fuelstation.controller;

import com.fuelstation.model.*;
import com.fuelstation.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/customer")
public class CustomerCommerceRestController {
    @Autowired private BillingService billing;
    @Autowired private OfferService offers;
    @Autowired private FuelService fuel;
    @GetMapping("/store") public Object store() {
        var active=offers.getActiveOffersByType(OfferTargetType.SPARE_PART);
        return billing.getAllSpareParts().stream().filter(p -> p.getStockQuantity()!=null && p.getStockQuantity()>0 && !"Inactive".equalsIgnoreCase(p.getStatus())).map(p -> {
            double discount=active.stream().filter(o -> p.getId().equals(o.getTargetId())).findFirst().map(Offer::getDiscountPercentage).orElse(0.0);
            return Map.of("id",p.getId(),"partName",p.getPartName(),"category",p.getCategory()==null ? "Parts" : p.getCategory(),"stockQuantity",p.getStockQuantity(),"sellingPrice",p.getSellingPrice(),"discountPercentage",discount,"price",com.fuelstation.util.Rules.money(p.getSellingPrice()*(1-discount/100)),"imageUrl",p.getImageUrl()==null?"":p.getImageUrl());
        }).toList();
    }
    public record Purchase(Long partId,Integer quantity,String paymentMethod,String licensePlate) {}
    @PostMapping("/store/purchase") public Object purchase(@RequestBody Purchase input,Authentication auth,@RequestHeader(value="Idempotency-Key",required=false) String key) {
        String method=com.fuelstation.util.Rules.paymentMethod(input.paymentMethod());
        if (!Set.of("CARD","QR","BANK").contains(method)) throw new IllegalArgumentException("Choose Card, QR or Bank for account payments");
        Invoice inv=billing.purchasePart(input.partId(),input.quantity(),auth.getName(),input.licensePlate(),null,method,null,key); return Map.of("success",true,"invoice",inv);
    }
    @GetMapping("/fuel") public Object fuel(Authentication auth) { return Map.of("prices",fuel.getAllPrices()); }
}
