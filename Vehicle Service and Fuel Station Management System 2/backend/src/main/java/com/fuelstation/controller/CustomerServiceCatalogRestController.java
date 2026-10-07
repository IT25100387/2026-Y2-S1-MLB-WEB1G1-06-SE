package com.fuelstation.controller;

import com.fuelstation.model.Offer;
import com.fuelstation.model.OfferTargetType;
import com.fuelstation.model.ServiceCatalogItem;
import com.fuelstation.service.OfferService;
import com.fuelstation.service.ServiceCatalogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customer/services")
public class CustomerServiceCatalogRestController {

    @Autowired
    private ServiceCatalogService serviceCatalogService;

    @Autowired
    private OfferService offerService;

    @GetMapping
    public ResponseEntity<List<ServiceCatalogItem>> getServiceCatalog() {
        List<ServiceCatalogItem> services = serviceCatalogService.getActiveServices();
        List<Offer> activeOffers = offerService.getActiveOffersByType(OfferTargetType.SERVICE);

        for (ServiceCatalogItem service : services) {
            for (Offer offer : activeOffers) {
                if (offer.getTargetId() != null && offer.getTargetId().equals(service.getId())) {
                    service.setDiscountPercentage(offer.getDiscountPercentage());
                    double discountAmount = (service.getEstimatedCost() * offer.getDiscountPercentage()) / 100.0;
                    service.setOfferPrice(service.getEstimatedCost() - discountAmount);
                    break;
                }
            }
        }
        
        return ResponseEntity.ok(services);
    }
}
