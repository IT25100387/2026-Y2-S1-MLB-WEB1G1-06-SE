package com.fuelstation.controller;

import com.fuelstation.model.DailyPumpSale;
import com.fuelstation.model.FuelInventory;
import com.fuelstation.model.FuelPrice;
import com.fuelstation.model.FuelPump;
import com.fuelstation.service.FuelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/fuel-operations")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class FuelOperationsRestController {

    @Autowired
    private FuelService fuelService;

    @GetMapping("/sales")
    public Map<String,Object> getSales() {
        fuelService.refreshStationDay();
        LocalDate day=fuelService.stationDay();
        List<DailyPumpSale> history=fuelService.getAllSalesHistory();
        var current=history.stream().filter(row -> day.equals(row.getSaleDate())).toList();
        var confirmed=current.stream().filter(row -> Boolean.TRUE.equals(row.getConfirmed())).toList();
        var totals=history.stream().filter(row -> Boolean.TRUE.equals(row.getConfirmed())).collect(java.util.stream.Collectors.groupingBy(DailyPumpSale::getSaleDate, java.util.TreeMap::new, java.util.stream.Collectors.summarizingDouble(row -> com.fuelstation.util.Rules.amount(row.getCollectedAmount()))));
        Map<String,Object> result=new HashMap<>(); result.put("pumps",fuelService.getAllPumps()); result.put("salesHistory",history); result.put("stationDay",day); result.put("dayStartsAt","05:00"); result.put("timeZone","Asia/Colombo"); result.put("currentFigures",current); result.put("confirmedPumpIds",confirmed.stream().map(row -> row.getPump().getId()).toList()); result.put("dailyCollection",confirmed.stream().mapToDouble(row -> row.getCollectedAmount()).sum()); result.put("dailyLitres",confirmed.stream().mapToDouble(row -> row.getLitersSold()).sum()); result.put("dailyDifference",confirmed.stream().mapToDouble(row -> row.getDifference()).sum()); result.put("collectionHistory",totals.entrySet().stream().map(entry -> Map.of("date",entry.getKey(),"collectedAmount",entry.getValue().getSum(),"confirmedPumps",entry.getValue().getCount())).toList()); result.put("totalRevenue",fuelService.getTotalRevenue()); return result;
    }
    public record DailyFigures(Long pumpId,String pumpStatus,String deactivationReason,Double litersSold,Double collectedAmount) {}
    @PostMapping("/sales/add")
    public Object confirm(@RequestBody DailyFigures input) { return Map.of("success",true,"sale",fuelService.recordDailyFigures(input.pumpId(),input.pumpStatus(),input.deactivationReason(),input.litersSold(),input.collectedAmount(),true)); }
    @PostMapping("/sales/draft")
    public Object draft(@RequestBody DailyFigures input) { return Map.of("success",true,"sale",fuelService.recordDailyFigures(input.pumpId(),input.pumpStatus(),input.deactivationReason(),input.litersSold(),input.collectedAmount(),false)); }

    @GetMapping("/pumps") public Object pumps(){return Map.of("pumps",fuelService.getAllPumps(),"inventories",fuelService.getAllInventories());}
    @PostMapping("/pumps") public Object addPump(@RequestBody FuelPump input){input.setId(null);return fuelService.savePump(input);}
    @PutMapping("/pumps/{id}") public Object editPump(@PathVariable Long id,@RequestBody FuelPump input){input.setId(id);return fuelService.savePump(input);}
    @DeleteMapping("/pumps/{id}") public Object deletePump(@PathVariable Long id){fuelService.deletePump(id);return Map.of("success",true);}

    // --- PRICES ---
    @GetMapping("/prices")
    public Map<String, Object> getPrices() {
        return Map.of("prices", fuelService.getAllPrices());
    }

    @PostMapping("/prices/add")
    public Map<String, Object> addPrice(@RequestBody FuelPrice price, org.springframework.security.core.Authentication auth) {
        if (price.getLastUpdated() == null) {
            price.setLastUpdated(java.time.LocalDateTime.now());
        }
        if (price.getUpdatedBy() == null) {
            price.setUpdatedBy(auth != null ? auth.getName() : "Admin");
        }
        price.setId(null);
        fuelService.savePrice(price);
        return Map.of("success", true);
    }

    @PostMapping("/prices/update/{id}")
    public Map<String, Object> updatePrice(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload,
            org.springframework.security.core.Authentication auth
    ) {
        Object value=payload.containsKey("currentPrice") ? payload.get("currentPrice") : payload.get("newPrice");
        if (!(value instanceof Number)) throw new IllegalArgumentException("Enter a valid fuel price");
        Double newPrice = ((Number)value).doubleValue();
        FuelPrice price = fuelService.getPriceById(id).orElseThrow(() -> new IllegalArgumentException("Fuel price not found"));
        price.setPreviousPrice(price.getCurrentPrice()); price.setCurrentPrice(newPrice); fuelService.savePrice(price);
        return Map.of("success", true);
    }

    @PostMapping("/prices/delete/{id}")
    public Map<String, Object> deletePrice(@PathVariable Long id) {
        fuelService.deletePrice(id);
        return Map.of("success", true);
    }
}
