package com.fuelstation.controller;

import com.fuelstation.model.FuelInventory;
import com.fuelstation.service.FuelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class FuelRestController {

    @Autowired
    private FuelService fuelService;

    @GetMapping("/fuel")
    public Map<String, Object> getFuelInventory() {
        Map<String, Object> response = new HashMap<>();
        response.put("inventories", fuelService.getAllInventories());
        return response;
    }

    @PostMapping("/fuel/add")
    public Map<String, Object> addFuelInventory(@RequestBody FuelInventory inv) {
        inv.setId(null);
        fuelService.saveInventory(inv);
        return Map.of("success", true);
    }

    @PostMapping("/fuel/update/{id}")
    public Map<String, Object> updateFuelInventory(@PathVariable Long id, @RequestBody FuelInventory inv) {
        inv.setId(id);
        fuelService.saveInventory(inv);
        return Map.of("success", true);
    }

    @PostMapping("/fuel/delete/{id}")
    public Map<String, Object> deleteFuelInventory(@PathVariable Long id) {
        fuelService.deleteInventory(id);
        return Map.of("success", true);
    }
}
