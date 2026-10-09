package com.fuelstation.service;

import com.fuelstation.model.Offer;
import com.fuelstation.model.OfferTargetType;
import com.fuelstation.model.SparePart;
import com.fuelstation.repository.OfferRepository;
import com.fuelstation.repository.SparePartRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@org.springframework.transaction.annotation.Transactional
public class OfferService {

    @Autowired
    private OfferRepository offerRepository;

    @Autowired
    private SparePartRepository sparePartRepository;

    @Autowired private com.fuelstation.repository.ServiceCatalogItemRepository services;

    public List<Offer> getAllOffersByType(OfferTargetType type) {
        return offerRepository.findByTargetType(type);
    }

    public List<Offer> getActiveOffersByType(OfferTargetType type) {
        List<Offer> activeOffers = offerRepository.findByTargetTypeAndActiveTrue(type);
        
        if (type == OfferTargetType.SPARE_PART) {
            // Filter out offers for spare parts that have 0 stock
            return activeOffers.stream().filter(offer -> {
                Optional<SparePart> partOpt = sparePartRepository.findById(offer.getTargetId());
                return partOpt.isPresent() && partOpt.get().getStockQuantity() != null && partOpt.get().getStockQuantity() > 0 && !"Inactive".equalsIgnoreCase(partOpt.get().getStatus());
            }).collect(Collectors.toList());
        }
        
        return activeOffers.stream().filter(o -> o.getTargetId() != null && services.findById(o.getTargetId()).map(s -> s.isActive()).orElse(false)).toList();
    }

    public Offer saveOffer(Offer offer) {
        if (offer.getTargetType() == null || offer.getTargetId() == null) throw new IllegalArgumentException("Select an offer target");
        boolean exists = offer.getTargetType() == OfferTargetType.SERVICE ? services.existsById(offer.getTargetId()) : sparePartRepository.existsById(offer.getTargetId());
        if (!exists) throw new IllegalArgumentException("Offer target does not exist");
        double discount = com.fuelstation.util.Rules.nonnegative(offer.getDiscountPercentage(), "Discount percentage");
        if (discount <= 0 || discount > 100) throw new IllegalArgumentException("Discount must be greater than 0 and at most 100%");
        offer.setDiscountPercentage(discount);
        if (offer.getId() != null && !offerRepository.existsById(offer.getId())) throw new IllegalArgumentException("Offer not found");
        if (offer.isActive() && offerRepository.findByTargetTypeAndActiveTrue(offer.getTargetType()).stream().anyMatch(o -> java.util.Objects.equals(o.getTargetId(), offer.getTargetId()) && !java.util.Objects.equals(o.getId(), offer.getId()))) throw new IllegalStateException("This item already has an active offer");
        return offerRepository.save(offer);
    }

    public Optional<Offer> getOfferById(Long id) { return offerRepository.findById(id); }

    public void deleteOffer(Long id) {
        if (!offerRepository.existsById(id)) throw new IllegalArgumentException("Offer not found");
        offerRepository.deleteById(id);
    }

    public void toggleActive(Long id) {
        Optional<Offer> opt = offerRepository.findById(id);
        if (opt.isEmpty()) throw new IllegalArgumentException("Offer not found");
        if (opt.isPresent()) {
            Offer offer = opt.get();
            offer.setActive(!offer.isActive());
            saveOffer(offer);
        }
    }
}
