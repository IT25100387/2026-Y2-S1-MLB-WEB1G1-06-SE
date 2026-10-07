package com.fuelstation.service;

import com.fuelstation.model.ServiceCatalogItem;
import com.fuelstation.repository.ServiceCatalogItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@org.springframework.transaction.annotation.Transactional
public class ServiceCatalogService {

    @Autowired
    private ServiceCatalogItemRepository repository;

    @Autowired private com.fuelstation.repository.ServiceBookingRepository bookings;
    @Autowired private com.fuelstation.repository.OfferRepository offers;

    public List<ServiceCatalogItem> getAllServices() {
        return repository.findAll();
    }

    public List<ServiceCatalogItem> getActiveServices() {
        return repository.findByActiveTrue();
    }

    public Optional<ServiceCatalogItem> getServiceById(Long id) {
        return repository.findById(id);
    }
    
    public Optional<ServiceCatalogItem> getServiceByName(String name) {
        return repository.findByNameIgnoreCase(name);
    }

    public ServiceCatalogItem saveService(ServiceCatalogItem service) {
        service.setName(com.fuelstation.util.Rules.required(service.getName(), "Service name"));
        service.setCategory(com.fuelstation.util.Rules.required(service.getCategory(), "Category"));
        service.setEstimatedCost(com.fuelstation.util.Rules.nonnegative(service.getEstimatedCost(), "Estimated cost"));
        com.fuelstation.service.SchedulingService.durationMinutes(service.getEstimatedDuration());
        var duplicate = repository.findByNameIgnoreCase(service.getName());
        if (duplicate.isPresent() && !java.util.Objects.equals(duplicate.get().getId(), service.getId())) throw new IllegalArgumentException("A service with this name already exists");
        if (service.getId() != null) { var old = repository.findById(service.getId()).orElseThrow(() -> new IllegalArgumentException("Service not found")); if (!old.getName().equals(service.getName()) && bookings.findAll().stream().anyMatch(b -> old.getName().equals(b.getServiceType()))) throw new IllegalStateException("This service has bookings. Keep its name to preserve those links."); }
        return repository.save(service);
    }

    public void deleteService(Long id) {
        var service = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Service not found"));
        if (bookings.findAll().stream().anyMatch(b -> service.getName().equals(b.getServiceType())) || offers.findAll().stream().anyMatch(o -> o.getTargetType()==com.fuelstation.model.OfferTargetType.SERVICE && id.equals(o.getTargetId()))) throw new IllegalStateException("Service has linked bookings or offers. Deactivate it instead.");
        repository.deleteById(id);
    }

    public void toggleActive(Long id) {
        Optional<ServiceCatalogItem> opt = repository.findById(id);
        if (opt.isPresent()) {
            ServiceCatalogItem item = opt.get();
            item.setActive(!item.isActive());
            repository.save(item);
        }
    }
}
