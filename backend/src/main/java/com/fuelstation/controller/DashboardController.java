package com.fuelstation.controller;

import com.fuelstation.model.FuelInventory;
import com.fuelstation.model.FuelPrice;
import com.fuelstation.model.FuelPump;
import com.fuelstation.service.FuelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

/** Retained admin JSON/text endpoint; dashboards are served by React. */
@RestController
public class DashboardController {
    @Autowired private FuelService fuelService;

    @GetMapping("/setup")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public String setupData() {
        if (fuelService.getAllInventories().isEmpty()) {
            FuelInventory i1 = new FuelInventory();
            i1.setFuelType("Lanka Petrol 92");
            i1.setCurrentStockLitres(5000.0);
            i1.setMaxCapacityLitres(10000.0);
            i1.setMinStockWarning(1000.0);
            i1.setStatus("Normal");
            fuelService.saveInventory(i1);

            FuelPrice p1 = new FuelPrice();
            p1.setFuelType("Lanka Petrol 92");
            p1.setCurrentPrice(340.0);
            p1.setPreviousPrice(350.0);
            p1.setLastUpdated(LocalDateTime.now());
            fuelService.savePrice(p1);

            FuelPump pump1 = new FuelPump();
            pump1.setPumpName("Pump 1");
            pump1.setPumpNumber("PUMP-01");
            pump1.setFuelType("Lanka Petrol 92");
            pump1.setStatus("Active");
            fuelService.savePump(pump1);

            return "Setup complete";
        }
        return "Already setup";
    }
}
