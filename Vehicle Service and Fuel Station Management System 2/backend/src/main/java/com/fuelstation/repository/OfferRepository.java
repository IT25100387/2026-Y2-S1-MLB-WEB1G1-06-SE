package com.fuelstation.repository;

import com.fuelstation.model.Offer;
import com.fuelstation.model.OfferTargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OfferRepository extends JpaRepository<Offer, Long> {
    List<Offer> findByTargetType(OfferTargetType targetType);
    List<Offer> findByTargetTypeAndActiveTrue(OfferTargetType targetType);
}
