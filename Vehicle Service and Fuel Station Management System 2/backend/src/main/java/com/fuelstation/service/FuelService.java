package com.fuelstation.service;

import com.fuelstation.model.FuelInventory;
import com.fuelstation.model.FuelPrice;
import com.fuelstation.model.FuelPump;

import java.util.List;
import java.util.Optional;

import com.fuelstation.model.DailyPumpSale;
import java.time.LocalDate;

public interface FuelService {
    // Sales
    double getTotalRevenue();

    LocalDate stationDay();
    void refreshStationDay();
    DailyPumpSale recordDailyFigures(Long pumpId, String status, String reason, Double litres, Double collected, boolean confirm);

    // Daily Pump Sales
    DailyPumpSale saveDailyPumpSale(DailyPumpSale dailyPumpSale);
    Optional<DailyPumpSale> getDailyPumpSale(Long pumpId, LocalDate date);
    List<DailyPumpSale> getPastWeekDailySales();
    List<DailyPumpSale> getAllSalesHistory();


    // Pumps
    List<FuelPump> getAllPumps();
    Optional<FuelPump> getPumpById(Long id);
    FuelPump savePump(FuelPump pump);
    void deletePump(Long id);
    long getPumpsCount();

    // Inventory
    List<FuelInventory> getAllInventories();
    Optional<FuelInventory> getInventoryById(Long id);
    FuelInventory saveInventory(FuelInventory inventory);
    void deleteInventory(Long id);

    // Prices
    List<FuelPrice> getAllPrices();
    Optional<FuelPrice> getPriceById(Long id);
    FuelPrice savePrice(FuelPrice price);
    void deletePrice(Long id);

    void seedDefaultFuelDataIfEmpty();
}
