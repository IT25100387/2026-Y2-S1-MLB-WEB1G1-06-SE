package com.fuelstation.controller;

import com.fuelstation.model.ServiceCatalogItem;
import com.fuelstation.service.ServiceCatalogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/services")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class ServiceCatalogRestController {

    @Autowired
    private ServiceCatalogService serviceCatalogRepository;
    @Autowired private com.fuelstation.service.CatalogPhotoStorage photos;

    @GetMapping
    public ResponseEntity<?> getAllServices() {
        List<ServiceCatalogItem> services = serviceCatalogRepository.getAllServices();
        Map<String, Object> response = new HashMap<>();
        response.put("services", services);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value="/add", consumes="application/json")
    public ResponseEntity<?> addService(@RequestBody ServiceCatalogItem serviceItem) {
        serviceItem.setId(null);
        serviceItem.setImageUrl(null);
        serviceItem.setActive(true);
        serviceCatalogRepository.saveService(serviceItem);
        return ResponseEntity.ok(Map.of("success", true, "message", "Service added successfully"));
    }

    @PostMapping(value="/update/{id}", consumes="application/json")
    public ResponseEntity<?> updateService(@PathVariable Long id, @RequestBody ServiceCatalogItem updatedItem) {
        ServiceCatalogItem item = serviceCatalogRepository.getServiceById(id).orElse(null);
        if (item == null) {
            return ResponseEntity.badRequest().body("Service not found");
        }
        item.setName(updatedItem.getName());
        item.setIcon(updatedItem.getIcon());
        item.setCategory(updatedItem.getCategory());
        item.setEstimatedCost(updatedItem.getEstimatedCost());
        item.setEstimatedDuration(updatedItem.getEstimatedDuration());
        item.setShortDescription(updatedItem.getShortDescription());
        item.setPopular(updatedItem.isPopular());
        
        serviceCatalogRepository.saveService(item);
        return ResponseEntity.ok(Map.of("success", true, "message", "Service updated successfully"));
    }

    @PostMapping(value="/add", consumes="multipart/form-data")
    public ResponseEntity<?> addWithPhoto(@RequestPart("service") ServiceCatalogItem item,
            @RequestPart(value="image",required=false) org.springframework.web.multipart.MultipartFile image) {
        item.setId(null); item.setActive(true);
        return saveWithPhoto(item, image, false);
    }

    @PostMapping(value="/update/{id}", consumes="multipart/form-data")
    public ResponseEntity<?> updateWithPhoto(@PathVariable Long id, @RequestPart("service") ServiceCatalogItem input,
            @RequestPart(value="image",required=false) org.springframework.web.multipart.MultipartFile image,
            @RequestParam(value="removeImage",defaultValue="false") boolean removeImage) {
        ServiceCatalogItem item = serviceCatalogRepository.getServiceById(id).orElseThrow(() -> new IllegalArgumentException("Service not found"));
        org.springframework.beans.BeanUtils.copyProperties(input, item, "id", "active", "imageUrl", "highlights", "discountPercentage", "offerPrice");
        return saveWithPhoto(item, image, removeImage);
    }

    private ResponseEntity<?> saveWithPhoto(ServiceCatalogItem item, org.springframework.web.multipart.MultipartFile image, boolean removeImage) {
        String uploaded = photos.store(image);
        if (uploaded != null) item.setImageUrl(uploaded);
        else if (removeImage) item.setImageUrl("");
        else if (item.getId() == null) item.setImageUrl(null);
        try { serviceCatalogRepository.saveService(item); return ResponseEntity.ok(Map.of("success",true)); }
        catch (RuntimeException error) { photos.discard(uploaded); throw error; }
    }

    @PostMapping("/toggle/{id}")
    public ResponseEntity<?> toggleService(@PathVariable Long id) {
        ServiceCatalogItem item = serviceCatalogRepository.getServiceById(id).orElse(null);
        if (item != null) {
            item.setActive(!item.isActive());
            serviceCatalogRepository.saveService(item);
            return ResponseEntity.ok(Map.of("success", true));
        }
        return ResponseEntity.badRequest().body("Service not found");
    }

    @PostMapping("/delete/{id}")
    public ResponseEntity<?> deleteService(@PathVariable Long id) {
        serviceCatalogRepository.deleteService(id);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
