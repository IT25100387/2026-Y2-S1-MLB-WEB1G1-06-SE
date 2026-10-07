package com.fuelstation.controller;

import com.fuelstation.model.Offer;
import com.fuelstation.model.OfferTargetType;
import com.fuelstation.model.ServiceCatalogItem;
import com.fuelstation.model.SparePart;
import com.fuelstation.service.OfferService;
import com.fuelstation.repository.ServiceCatalogItemRepository;
import com.fuelstation.repository.SparePartRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/offers")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class OfferRestController {

    @Autowired
    private OfferService offerRepository;

    @Autowired
    private SparePartRepository sparePartRepository;
    
    @Autowired
    private ServiceCatalogItemRepository serviceCatalogRepository;

    // Get all spare part offers
    @GetMapping("/spare-parts")
    public ResponseEntity<?> getSparePartOffers() {
        List<Offer> offers = offerRepository.getAllOffersByType(OfferTargetType.SPARE_PART);
        List<SparePart> spareParts = sparePartRepository.findAll();
        
        Map<String, Object> response = new HashMap<>();
        response.put("offers", offers);
        response.put("spareParts", spareParts);
        return ResponseEntity.ok(response);
    }

    // Add new spare part offer with image
    @PostMapping("/spare-parts/add")
    public ResponseEntity<?> addSparePartOffer(
            @RequestParam("targetId") Long targetId,
            @RequestParam("discountPercentage") Double discountPercentage,
            @RequestParam(value = "image", required = false) MultipartFile image) {
        
        Offer offer = new Offer();
        offer.setTargetType(OfferTargetType.SPARE_PART);
        offer.setTargetId(targetId);
        offer.setDiscountPercentage(discountPercentage);
        offer.setActive(true);

        if (image != null && !image.isEmpty()) {
            try {
                String original = image.getOriginalFilename();
                String extension = original != null && original.contains(".") ? original.substring(original.lastIndexOf(".")).toLowerCase() : "";
                if (!java.util.Set.of(".png", ".jpg", ".jpeg", ".webp").contains(extension) || image.getSize() > 5_000_000) throw new IllegalArgumentException("Use a PNG, JPEG or WebP image under 5 MB");
                String fileName = java.util.UUID.randomUUID() + extension;
                String uniqueFileName = System.currentTimeMillis() + "_" + fileName;
                Path uploadPath = Paths.get("uploads/offers");
                
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }
                
                Path filePath = uploadPath.resolve(uniqueFileName);
                Files.copy(image.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                
                offer.setImageUrl("/uploads/offers/" + uniqueFileName);
            } catch (IOException e) {
                return ResponseEntity.internalServerError().body("Could not upload image: " + e.getMessage());
            }
        }

        offerRepository.saveOffer(offer);
        return ResponseEntity.ok(Map.of("success", true, "message", "Offer created successfully"));
    }

    @PostMapping("/spare-parts/update/{id}")
    public ResponseEntity<?> updateSparePartOffer(
            @PathVariable Long id,
            @RequestParam("discountPercentage") Double discountPercentage,
            @RequestParam(value = "image", required = false) MultipartFile image) {
        
        Offer offer = offerRepository.getOfferById(id).orElse(null);
        if (offer == null) {
            return ResponseEntity.badRequest().body("Offer not found");
        }
        
        offer.setDiscountPercentage(discountPercentage);

        if (image != null && !image.isEmpty()) {
            try {
                String original = image.getOriginalFilename();
                String extension = original != null && original.contains(".") ? original.substring(original.lastIndexOf(".")).toLowerCase() : "";
                if (!java.util.Set.of(".png", ".jpg", ".jpeg", ".webp").contains(extension) || image.getSize() > 5_000_000) throw new IllegalArgumentException("Use a PNG, JPEG or WebP image under 5 MB");
                String fileName = java.util.UUID.randomUUID() + extension;
                String uniqueFileName = System.currentTimeMillis() + "_" + fileName;
                Path uploadPath = Paths.get("uploads/offers");
                
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }
                
                Path filePath = uploadPath.resolve(uniqueFileName);
                Files.copy(image.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                
                offer.setImageUrl("/uploads/offers/" + uniqueFileName);
            } catch (IOException e) {
                return ResponseEntity.internalServerError().body("Could not upload image: " + e.getMessage());
            }
        }

        offerRepository.saveOffer(offer);
        return ResponseEntity.ok(Map.of("success", true, "message", "Offer updated successfully"));
    }

    // Toggle active status
    @PostMapping("/spare-parts/toggle/{id}")
    public ResponseEntity<?> toggleOffer(@PathVariable Long id) {
        Offer offer = offerRepository.getOfferById(id).orElse(null);
        if (offer != null) {
            offer.setActive(!offer.isActive());
            offerRepository.saveOffer(offer);
            return ResponseEntity.ok(Map.of("success", true));
        }
        return ResponseEntity.badRequest().body("Offer not found");
    }

    // Delete spare part offer
    @PostMapping("/spare-parts/delete/{id}")
    public ResponseEntity<?> deleteSparePartOffer(@PathVariable Long id) {
        offerRepository.deleteOffer(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // --- Service Offers ---

    @GetMapping("/services")
    public ResponseEntity<?> getServiceOffers() {
        List<Offer> offers = offerRepository.getAllOffersByType(OfferTargetType.SERVICE);
        List<ServiceCatalogItem> services = serviceCatalogRepository.findByActiveTrue();
        
        Map<String, Object> response = new HashMap<>();
        response.put("offers", offers);
        response.put("services", services);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/services/add")
    public ResponseEntity<?> addServiceOffer(
            @RequestParam("targetId") Long targetId,
            @RequestParam("discountPercentage") Double discountPercentage,
            @RequestParam(value = "image", required = false) MultipartFile image) {
        
        Offer offer = new Offer();
        offer.setTargetType(OfferTargetType.SERVICE);
        offer.setTargetId(targetId);
        offer.setDiscountPercentage(discountPercentage);
        offer.setActive(true);

        if (image != null && !image.isEmpty()) {
            try {
                String original = image.getOriginalFilename();
                String extension = original != null && original.contains(".") ? original.substring(original.lastIndexOf(".")).toLowerCase() : "";
                if (!java.util.Set.of(".png", ".jpg", ".jpeg", ".webp").contains(extension) || image.getSize() > 5_000_000) throw new IllegalArgumentException("Use a PNG, JPEG or WebP image under 5 MB");
                String fileName = java.util.UUID.randomUUID() + extension;
                String uniqueFileName = System.currentTimeMillis() + "_" + fileName;
                Path uploadPath = Paths.get("uploads/offers");
                
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }
                
                Path filePath = uploadPath.resolve(uniqueFileName);
                Files.copy(image.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                
                offer.setImageUrl("/uploads/offers/" + uniqueFileName);
            } catch (IOException e) {
                return ResponseEntity.internalServerError().body("Could not upload image: " + e.getMessage());
            }
        }

        offerRepository.saveOffer(offer);
        return ResponseEntity.ok(Map.of("success", true, "message", "Service Offer created successfully"));
    }

    @PostMapping("/services/update/{id}")
    public ResponseEntity<?> updateServiceOffer(
            @PathVariable Long id,
            @RequestParam("discountPercentage") Double discountPercentage,
            @RequestParam(value = "image", required = false) MultipartFile image) {
        
        Offer offer = offerRepository.getOfferById(id).orElse(null);
        if (offer == null) {
            return ResponseEntity.badRequest().body("Offer not found");
        }
        
        offer.setDiscountPercentage(discountPercentage);

        if (image != null && !image.isEmpty()) {
            try {
                String original = image.getOriginalFilename();
                String extension = original != null && original.contains(".") ? original.substring(original.lastIndexOf(".")).toLowerCase() : "";
                if (!java.util.Set.of(".png", ".jpg", ".jpeg", ".webp").contains(extension) || image.getSize() > 5_000_000) throw new IllegalArgumentException("Use a PNG, JPEG or WebP image under 5 MB");
                String fileName = java.util.UUID.randomUUID() + extension;
                String uniqueFileName = System.currentTimeMillis() + "_" + fileName;
                Path uploadPath = Paths.get("uploads/offers");
                
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }
                
                Path filePath = uploadPath.resolve(uniqueFileName);
                Files.copy(image.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                
                offer.setImageUrl("/uploads/offers/" + uniqueFileName);
            } catch (IOException e) {
                return ResponseEntity.internalServerError().body("Could not upload image: " + e.getMessage());
            }
        }

        offerRepository.saveOffer(offer);
        return ResponseEntity.ok(Map.of("success", true, "message", "Service Offer updated successfully"));
    }

    @PostMapping("/services/toggle/{id}")
    public ResponseEntity<?> toggleServiceOffer(@PathVariable Long id) {
        Offer offer = offerRepository.getOfferById(id).orElse(null);
        if (offer != null) {
            offer.setActive(!offer.isActive());
            offerRepository.saveOffer(offer);
            return ResponseEntity.ok(Map.of("success", true));
        }
        return ResponseEntity.badRequest().body("Offer not found");
    }

    @PostMapping("/services/delete/{id}")
    public ResponseEntity<?> deleteServiceOffer(@PathVariable Long id) {
        offerRepository.deleteOffer(id);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
