package com.fuelstation.repository;

import com.fuelstation.model.ServiceCatalogItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceCatalogItemRepository extends JpaRepository<ServiceCatalogItem, Long> {
    List<ServiceCatalogItem> findByActiveTrue();
    Optional<ServiceCatalogItem> findByNameIgnoreCase(String name);
}
