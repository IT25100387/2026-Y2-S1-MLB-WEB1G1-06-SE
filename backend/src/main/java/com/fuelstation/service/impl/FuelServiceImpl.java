package com.fuelstation.service.impl;

import com.fuelstation.model.*;
import com.fuelstation.repository.*;
import com.fuelstation.service.*;
import com.fuelstation.util.Rules;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import java.time.*;
import java.util.*;

@Service
@Transactional
public class FuelServiceImpl implements FuelService {
    @Autowired private FuelSaleRepository saleRepository;
    @Autowired private FuelInventoryRepository inventoryRepository;
    @Autowired private DailyPumpSaleRepository dailyPumpSaleRepository;
    @Autowired private FuelPumpRepository pumpRepository;
    @Autowired private FuelPriceRepository priceRepository;
    @Autowired private SupplierRepository suppliers;
    @Autowired private BillingService billing;
    @Autowired private NotificationService notices;
    @Autowired private Clock clock;
    private String actor() { var a=org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication(); return a==null ? "system" : a.getName(); }
    @Override public LocalDate stationDay() { return LocalDateTime.now(clock).minusHours(5).toLocalDate(); }
    @Override @Scheduled(cron="0 0 5 * * *", zone="${app.station.time-zone:Asia/Colombo}")
    public void refreshStationDay() {
        LocalDate day=stationDay();
        for (FuelPump item : pumpRepository.findAll()) {
            FuelPump pump=pumpRepository.lockById(item.getId()).orElseThrow();
            if (!day.equals(pump.getStationDate())) {
                pump.setOpeningReading(Rules.amount(pump.getCurrentMeterReading())); pump.setClosingReading(null); pump.setTodaysSales(0.0); pump.setStationDate(day); pumpRepository.save(pump);
            }
            if (dailyPumpSaleRepository.findByPumpIdAndSaleDate(pump.getId(),day).isEmpty()) {
                DailyPumpSale draft=new DailyPumpSale(); draft.setPump(pump); draft.setSaleDate(day); draft.setPumpStatus("Active".equals(pump.getStatus()) ? "Active" : "Inactive"); draft.setLitersSold(0.0); draft.setExpectedAmount(0.0); draft.setCollectedAmount(0.0); draft.setDifference(0.0); draft.setUnitPrice(priceRepository.findByFuelTypeIgnoreCase(pump.getFuelType()).map(FuelPrice::getCurrentPrice).orElse(0.0)); dailyPumpSaleRepository.save(draft);
            }
        }
    }
    @Override public DailyPumpSale recordDailyFigures(Long pumpId, String status, String reason, Double litres, Double collected, boolean confirm) {
        refreshStationDay();
        FuelPump pump=pumpRepository.lockById(pumpId).orElseThrow(() -> new IllegalArgumentException("Pump not found"));
        DailyPumpSale row=dailyPumpSaleRepository.findByPumpIdAndSaleDate(pumpId,stationDay()).orElseThrow();
        if (Boolean.TRUE.equals(row.getConfirmed())) throw new IllegalStateException("This pump's figures are already confirmed for this station day");
        String state=Rules.required(status,"Pump status");
        boolean active="Active".equalsIgnoreCase(state);
        if (!active && !Set.of("Inactive","Deactive","Deactivated","Maintenance","Disabled").contains(state)) throw new IllegalArgumentException("Invalid pump status");
        double qty=Rules.nonnegative(litres,"Litres sold"), money=Rules.nonnegative(collected,"Collected amount");
        if (!active) { if (confirm) reason=Rules.required(reason,"Deactivation reason"); if (qty>0 || money>0) throw new IllegalArgumentException("Inactive pumps cannot have daily sales figures"); }
        FuelPrice price=priceRepository.findByFuelTypeIgnoreCase(pump.getFuelType()).orElseThrow(() -> new IllegalStateException("Configure the pump's fuel price first"));
        double unit=Rules.positive(price.getCurrentPrice(),"Fuel price");
        row.setPumpStatus(active ? "Active" : "Inactive"); row.setDeactivationReason(active ? null : reason); row.setLitersSold(qty); row.setUnitPrice(unit); row.setExpectedAmount(Rules.money(qty*unit)); row.setCollectedAmount(money); row.setDifference(Rules.money(money-row.getExpectedAmount()));
        if (confirm) {
            if (active && qty>0) consume(pump.getFuelType(),qty);
            row.setConfirmed(true); row.setConfirmedAt(LocalDateTime.now(clock)); row.setRecordedBy(actor());
            pump.setStatus(active ? "Active" : "Maintenance"); pump.setTodaysSales(qty); pump.setClosingReading(Rules.amount(pump.getOpeningReading())+qty); pump.setCurrentMeterReading(pump.getClosingReading()); pumpRepository.save(pump);
            notices.notifyRoles("Pump figures confirmed", pump.getPumpName()+" ? "+stationDay()+" ? collected Rs. "+money, "Fuel", "/dashboard/fuel/sales", "MANAGER");
        }
        return dailyPumpSaleRepository.save(row);
    }
    private void consume(String fuelType,double litres) {
        FuelInventory found=inventoryRepository.findByFuelType(fuelType).orElseThrow(() -> new IllegalStateException("Fuel tank not configured"));
        FuelInventory tank=inventoryRepository.lockById(found.getId()).orElseThrow();
        if (tank.getCurrentStockLitres()==null || litres>tank.getCurrentStockLitres()) throw new IllegalStateException("Insufficient fuel stock. Available: "+tank.getCurrentStockLitres()+" L");
        tank.setCurrentStockLitres(Rules.money(tank.getCurrentStockLitres()-litres)); tank.setStatus(tank.getCurrentStockLitres()<=Rules.amount(tank.getMinStockWarning()) ? "Low Stock" : "Normal"); inventoryRepository.save(tank);
        if ("Low Stock".equals(tank.getStatus())) notices.notifyRoles("Fuel stock low",fuelType+" has "+tank.getCurrentStockLitres()+" L remaining", "Fuel", "/dashboard/inventory/fuel", "MANAGER");
    }
    @Override @Transactional(readOnly=true) public double getTotalRevenue() { return Rules.money(billing.getAllInvoices().stream().filter(i -> "FUEL".equals(i.getInvoiceType())).mapToDouble(i -> Rules.amount(i.getAmountPaid())).sum()+dailyPumpSaleRepository.findAll().stream().filter(s -> Boolean.TRUE.equals(s.getConfirmed())).mapToDouble(s -> Rules.amount(s.getCollectedAmount())).sum()); }
    @Override public DailyPumpSale saveDailyPumpSale(DailyPumpSale s) { if (s.getPump()==null) throw new IllegalArgumentException("Pump is required"); return recordDailyFigures(s.getPump().getId(),s.getPumpStatus(),s.getDeactivationReason(),s.getLitersSold(),s.getCollectedAmount(),Boolean.TRUE.equals(s.getConfirmed())); }
    @Override @Transactional(readOnly=true) public Optional<DailyPumpSale> getDailyPumpSale(Long pumpId,LocalDate date) { return dailyPumpSaleRepository.findByPumpIdAndSaleDate(pumpId,date); }
    @Override @Transactional(readOnly=true) public List<DailyPumpSale> getPastWeekDailySales() { return dailyPumpSaleRepository.findBySaleDateGreaterThanEqualOrderBySaleDateDesc(stationDay().minusDays(6)); }
    @Override @Transactional(readOnly=true) public List<DailyPumpSale> getAllSalesHistory() { return dailyPumpSaleRepository.findAllByOrderBySaleDateDesc(); }
    @Override @Transactional(readOnly=true) public List<FuelPump> getAllPumps() { return pumpRepository.findAll(); }
    @Override @Transactional(readOnly=true) public Optional<FuelPump> getPumpById(Long id) { return pumpRepository.findById(id); }
    @Override public FuelPump savePump(FuelPump input) {
        FuelPump row=input.getId()==null ? new FuelPump() : pumpRepository.lockById(input.getId()).orElseThrow(() -> new IllegalArgumentException("Pump not found"));
        input.setPumpName(Rules.required(input.getPumpName(),"Pump name")); input.setPumpNumber(Rules.required(input.getPumpNumber(),"Pump number")); input.setFuelType(Rules.required(input.getFuelType(),"Fuel type"));
        if (priceRepository.findByFuelTypeIgnoreCase(input.getFuelType()).isEmpty() || inventoryRepository.findByFuelType(input.getFuelType()).isEmpty()) throw new IllegalArgumentException("Configure this fuel's price and tank first");
        if (pumpRepository.findAll().stream().anyMatch(p -> !Objects.equals(p.getId(),row.getId()) && (p.getPumpName().equalsIgnoreCase(input.getPumpName()) || p.getPumpNumber().equalsIgnoreCase(input.getPumpNumber())))) throw new IllegalArgumentException("Pump name and number must be unique");
        boolean hasHistory=row.getId()!=null && (dailyPumpSaleRepository.findAll().stream().anyMatch(s -> row.getId().equals(s.getPump().getId())) || saleRepository.findAll().stream().anyMatch(s -> row.getPumpName().equals(s.getPumpName())));
        if (hasHistory && (!Objects.equals(row.getPumpName(),input.getPumpName()) || !Objects.equals(row.getPumpNumber(),input.getPumpNumber()) || !Objects.equals(row.getFuelType(),input.getFuelType()))) throw new IllegalStateException("Keep the pump identifiers and fuel type to preserve its sales history. Disable it and create a new pump when replacing it.");
        if (input.getStatus()==null) input.setStatus("Active");
        if (!Set.of("Active","Maintenance","Disabled","Inactive").contains(input.getStatus())) throw new IllegalArgumentException("Invalid pump status");
        org.springframework.beans.BeanUtils.copyProperties(input,row,"id","stationDate","openingReading","closingReading","todaysSales","currentMeterReading");
        return pumpRepository.save(row);
    }
    @Override public void deletePump(Long id) { FuelPump pump=pumpRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Pump not found")); if (dailyPumpSaleRepository.findAll().stream().anyMatch(s -> id.equals(s.getPump().getId())) || saleRepository.findAll().stream().anyMatch(s -> pump.getPumpName().equals(s.getPumpName()))) throw new IllegalStateException("Pump has sales history. Disable it instead."); pumpRepository.deleteById(id); }
    @Override @Transactional(readOnly=true) public long getPumpsCount() { return pumpRepository.count(); }
    @Override @Transactional(readOnly=true) public List<FuelInventory> getAllInventories() { return inventoryRepository.findAll(); }
    @Override @Transactional(readOnly=true) public Optional<FuelInventory> getInventoryById(Long id) { return inventoryRepository.findById(id); }
    @Override public FuelInventory saveInventory(FuelInventory input) {
        input.setFuelType(Rules.required(input.getFuelType(),"Fuel type")); input.setCurrentStockLitres(Rules.nonnegative(input.getCurrentStockLitres(),"Stock")); input.setMaxCapacityLitres(Rules.positive(input.getMaxCapacityLitres(),"Capacity")); input.setMinStockWarning(Rules.nonnegative(input.getMinStockWarning(),"Warning level"));
        if (input.getCurrentStockLitres()>input.getMaxCapacityLitres() || input.getMinStockWarning()>input.getMaxCapacityLitres()) throw new IllegalArgumentException("Stock and warning level cannot exceed tank capacity");
        if (input.getSupplier()!=null && !input.getSupplier().isBlank() && suppliers.findAll().stream().noneMatch(s -> s.getSupplierName().equals(input.getSupplier()))) throw new IllegalArgumentException("Select an existing supplier");
        FuelInventory row=input.getId()==null ? new FuelInventory() : inventoryRepository.lockById(input.getId()).orElseThrow(() -> new IllegalArgumentException("Fuel tank not found"));
        if (row.getId()!=null && !Objects.equals(row.getFuelType(),input.getFuelType()) && pumpRepository.findAll().stream().anyMatch(p -> row.getFuelType().equals(p.getFuelType()))) throw new IllegalStateException("Tank fuel type is used by a pump");
        if (input.getVersion()!=null && !Objects.equals(input.getVersion(),row.getVersion())) throw new IllegalStateException("Fuel stock changed while this form was open. Refresh the tank and try again.");
        org.springframework.beans.BeanUtils.copyProperties(input,row,"id","version"); row.setStatus(row.getCurrentStockLitres()<=row.getMinStockWarning() ? "Low Stock" : "Normal"); return inventoryRepository.save(row);
    }
    @Override public void deleteInventory(Long id) { FuelInventory row=inventoryRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Fuel tank not found")); if (pumpRepository.findAll().stream().anyMatch(p -> row.getFuelType().equals(p.getFuelType()))) throw new IllegalStateException("Tank is linked to a pump"); inventoryRepository.deleteById(id); }
    @Override @Transactional(readOnly=true) public List<FuelPrice> getAllPrices() { return priceRepository.findAll(); }
    @Override @Transactional(readOnly=true) public Optional<FuelPrice> getPriceById(Long id) { return priceRepository.findById(id); }
    @Override public FuelPrice savePrice(FuelPrice input) {
        input.setFuelType(Rules.required(input.getFuelType(),"Fuel type")); input.setCurrentPrice(Rules.positive(input.getCurrentPrice(),"Fuel price"));
        if (input.getId()!=null) { var existing=priceRepository.findById(input.getId()).orElseThrow(() -> new IllegalArgumentException("Fuel price not found")); if (!existing.getFuelType().equals(input.getFuelType()) && (pumpRepository.findAll().stream().anyMatch(p -> existing.getFuelType().equals(p.getFuelType())) || inventoryRepository.findByFuelType(existing.getFuelType()).isPresent())) throw new IllegalStateException("Keep the fuel type to preserve linked pump and tank pricing"); }
        var old=priceRepository.findByFuelTypeIgnoreCase(input.getFuelType()); if (old.isPresent() && !Objects.equals(old.get().getId(),input.getId())) throw new IllegalArgumentException("Fuel already has a price entry");
        input.setLastUpdated(LocalDateTime.now(clock)); input.setUpdatedBy(actor()); return priceRepository.save(input);
    }
    @Override public void deletePrice(Long id) { FuelPrice row=priceRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Fuel price not found")); if (pumpRepository.findAll().stream().anyMatch(p -> row.getFuelType().equals(p.getFuelType()))) throw new IllegalStateException("Price is linked to a pump"); priceRepository.deleteById(id); }
    @Override public void seedDefaultFuelDataIfEmpty() {
        if (priceRepository.count()>0 || inventoryRepository.count()>0 || pumpRepository.count()>0) return;
        ensureFuelPrice("Lanka Petrol 92",340.0,350.0); ensureFuelPrice("Lanka Petrol 95 (Euro 4)",385.0,395.0); ensureFuelPrice("Lanka Auto Diesel",325.0,335.0); ensureFuelPrice("Lanka Super Diesel (Euro 4)",360.0,370.0);
        ensureFuelInventory("Lanka Petrol 92",14500.0,25000.0,2500.0); ensureFuelInventory("Lanka Petrol 95 (Euro 4)",10200.0,20000.0,2000.0); ensureFuelInventory("Lanka Auto Diesel",18000.0,30000.0,3000.0); ensureFuelInventory("Lanka Super Diesel (Euro 4)",9800.0,18000.0,1800.0);
        ensureFuelPump("Pump 01","PUMP-01","Lanka Petrol 92"); ensureFuelPump("Pump 02","PUMP-02","Lanka Petrol 95 (Euro 4)"); ensureFuelPump("Pump 03","PUMP-03","Lanka Auto Diesel"); ensureFuelPump("Pump 04","PUMP-04","Lanka Super Diesel (Euro 4)");
    }
    private void ensureFuelPrice(String fuelType, Double currentPrice, Double previousPrice) {
        Optional<FuelPrice> opt = priceRepository.findByFuelTypeIgnoreCase(fuelType);
        if (opt.isEmpty()) {
            FuelPrice p = new FuelPrice();
            p.setFuelType(fuelType);
            p.setCurrentPrice(currentPrice);
            p.setPreviousPrice(previousPrice);
            p.setLastUpdated(LocalDateTime.now(clock).minusDays(1));
            p.setUpdatedBy("Admin (Pricing Committee)");
            savePrice(p);
        }
    }

    private void ensureFuelInventory(String fuelType, Double stock, Double maxCap, Double minWarning) {
        Optional<FuelInventory> opt = inventoryRepository.findByFuelType(fuelType);
        if (opt.isEmpty()) {
            FuelInventory inv = new FuelInventory();
            inv.setFuelType(fuelType);
            inv.setCurrentStockLitres(stock);
            inv.setMaxCapacityLitres(maxCap);
            inv.setMinStockWarning(minWarning);
            inv.setStatus("Normal");
            saveInventory(inv);
        }
    }

    private void ensureFuelPump(String name, String number, String fuelType) {
        boolean exists = pumpRepository.findAll().stream()
                .anyMatch(p -> name.equalsIgnoreCase(p.getPumpName()) || number.equalsIgnoreCase(p.getPumpNumber()));
        if (!exists) {
            FuelPump pump = new FuelPump();
            pump.setPumpName(name);
            pump.setPumpNumber(number);
            pump.setFuelType(fuelType);
            pump.setStatus("Active");
            savePump(pump);
        }
    }
}
