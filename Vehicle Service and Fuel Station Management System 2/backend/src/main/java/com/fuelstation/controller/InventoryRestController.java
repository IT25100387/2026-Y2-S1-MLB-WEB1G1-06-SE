package com.fuelstation.controller;

import com.fuelstation.model.SparePart;
import com.fuelstation.model.Supplier;
import com.fuelstation.service.BillingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class InventoryRestController {

    @Autowired
    private BillingService billingService;
    @Autowired private com.fuelstation.service.CatalogPhotoStorage photos;

    // --- SUPPLIERS ---
    @GetMapping("/suppliers")
    public Map<String, Object> getSuppliers() {
        Map<String, Object> response = new HashMap<>();
        response.put("suppliers", billingService.getAllSuppliers());
        response.put("parts", billingService.getAllSpareParts());
        return response;
    }

    @PostMapping("/suppliers/add")
    public Map<String, Object> addSupplier(@RequestBody Supplier supplier) {
        supplier.setId(null);
        billingService.saveSupplier(supplier);
        return Map.of("success", true);
    }

    @PostMapping("/suppliers/update/{id}")
    public Map<String, Object> updateSupplier(@PathVariable Long id, @RequestBody Supplier supplier) {
        supplier.setId(id);
        billingService.saveSupplier(supplier);
        return Map.of("success", true);
    }

    @PostMapping("/suppliers/delete/{id}")
    public Map<String, Object> deleteSupplier(@PathVariable Long id) {
        billingService.deleteSupplier(id);
        return Map.of("success", true);
    }

    // --- SPARE PARTS ---
    @GetMapping("/parts")
    public Map<String, Object> getSpareParts() {
        Map<String, Object> response = new HashMap<>();
        response.put("parts", billingService.getAllSpareParts());
        return response;
    }

    @PostMapping(value="/parts/add", consumes="application/json")
    public Map<String, Object> addSparePart(@RequestBody SparePart part) {
        part.setId(null); part.setVersion(null);
        part.setImageUrl(null);
        billingService.saveSparePart(part);
        return Map.of("success", true);
    }

    @PostMapping(value="/parts/update/{id}", consumes="application/json")
    public Map<String, Object> updateSparePart(@PathVariable Long id, @RequestBody SparePart part) {
        part.setId(id);
        part.setImageUrl(null); // JSON-only callers preserve the saved photo.
        billingService.saveSparePart(part);
        return Map.of("success", true);
    }

    @PostMapping(value="/parts/add", consumes="multipart/form-data")
    public Map<String,Object> addPartWithPhoto(@RequestPart("part") SparePart part,
            @RequestPart(value="image",required=false) org.springframework.web.multipart.MultipartFile image) {
        part.setId(null); part.setVersion(null);
        return savePartWithPhoto(part, image, false);
    }

    @PostMapping(value="/parts/update/{id}", consumes="multipart/form-data")
    public Map<String,Object> updatePartWithPhoto(@PathVariable Long id, @RequestPart("part") SparePart part,
            @RequestPart(value="image",required=false) org.springframework.web.multipart.MultipartFile image,
            @RequestParam(value="removeImage",defaultValue="false") boolean removeImage) {
        part.setId(id);
        return savePartWithPhoto(part, image, removeImage);
    }

    private Map<String,Object> savePartWithPhoto(SparePart part, org.springframework.web.multipart.MultipartFile image, boolean removeImage) {
        String uploaded = photos.store(image);
        part.setImageUrl(uploaded != null ? uploaded : removeImage ? "" : null);
        try { billingService.saveSparePart(part); return Map.of("success", true); }
        catch (RuntimeException error) { photos.discard(uploaded); throw error; }
    }

    @PostMapping("/parts/delete/{id}")
    public Map<String, Object> deleteSparePart(@PathVariable Long id) {
        billingService.deleteSparePart(id);
        return Map.of("success", true);
    }
}
