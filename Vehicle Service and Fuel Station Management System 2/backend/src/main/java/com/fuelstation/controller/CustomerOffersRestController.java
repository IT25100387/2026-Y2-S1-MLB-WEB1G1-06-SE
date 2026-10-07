package com.fuelstation.controller;

import com.fuelstation.model.*;
import com.fuelstation.service.BillingService;
import com.fuelstation.service.OfferService;
import com.fuelstation.service.ServiceCatalogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/customer/offers")
public class CustomerOffersRestController {

    @Autowired
    private OfferService offerService;

    @Autowired
    private ServiceCatalogService serviceCatalogService;

    @Autowired
    private BillingService billingService;

    public static class OfferDisplayDTO {
        public String name;
        public String description;
        public String category;
        public Double originalPrice;
        public Double discountPercentage;
        public Double newPrice;
        public String icon;

        public OfferDisplayDTO(String name, String description, String category, Double originalPrice, Double discountPercentage, String icon) {
            this.name = name;
            this.description = description;
            this.category = category;
            this.originalPrice = originalPrice;
            this.discountPercentage = discountPercentage;
            this.icon = icon;
            
            double discountAmt = (originalPrice * discountPercentage) / 100.0;
            this.newPrice = originalPrice - discountAmt;
        }
    }

    @GetMapping
    public ResponseEntity<?> getOffers() {
        List<Offer> activeServiceOffers = offerService.getActiveOffersByType(OfferTargetType.SERVICE);
        List<Offer> activePartOffers = offerService.getActiveOffersByType(OfferTargetType.SPARE_PART);

        List<OfferDisplayDTO> serviceOfferDTOs = new ArrayList<>();
        for (Offer offer : activeServiceOffers) {
            Optional<ServiceCatalogItem> itemOpt = serviceCatalogService.getServiceById(offer.getTargetId());
            if (itemOpt.isPresent()) {
                ServiceCatalogItem item = itemOpt.get();
                if (item.isActive()) {
                    serviceOfferDTOs.add(new OfferDisplayDTO(
                        item.getName(),
                        item.getShortDescription(),
                        item.getCategory(),
                        item.getEstimatedCost(),
                        offer.getDiscountPercentage(),
                        item.getIcon() != null ? item.getIcon() : "fa-wrench"
                    ));
                }
            }
        }

        List<OfferDisplayDTO> partOfferDTOs = new ArrayList<>();
        for (Offer offer : activePartOffers) {
            Optional<SparePart> partOpt = billingService.getSparePartById(offer.getTargetId());
            if (partOpt.isPresent()) {
                SparePart part = partOpt.get();
                // Out of Stock Protection
                if (part.getStockQuantity() != null && part.getStockQuantity() > 0) {
                    partOfferDTOs.add(new OfferDisplayDTO(
                        part.getPartName(),
                        "Supplier: " + part.getSupplier(),
                        part.getCategory(),
                        part.getSellingPrice(),
                        offer.getDiscountPercentage(),
                        "fa-gears"
                    ));
                }
            }
        }

        return ResponseEntity.ok(Map.of(
            "serviceOffers", serviceOfferDTOs,
            "sparePartOffers", partOfferDTOs
        ));
    }
}
