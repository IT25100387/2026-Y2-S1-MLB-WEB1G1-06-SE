package com.fuelstation.config;

import com.fuelstation.model.*;
import com.fuelstation.service.*;
import com.fuelstation.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;

@Component @Order(2) @ConditionalOnProperty(name="app.demo.seed",havingValue="true")
public class ValidDemoDataInitializer implements CommandLineRunner {
    @Autowired UserService users; @Autowired VehicleService vehicles; @Autowired StaffService hr;
    @Autowired WorkshopService workshop; @Autowired BookingService bookings; @Autowired BillingService billing;
    @Autowired ServiceCatalogService catalog; @Autowired OfferService offers; @Autowired FuelService fuel;
    @Autowired ServiceBookingRepository bookingRows; @Autowired StaffRepository staffRows; @Autowired Clock clock;
    @Override @Transactional public void run(String... args) {
        // Only initialize an empty workspace. Restarts never recreate deleted/edited business records.
        if (!catalog.getAllServices().isEmpty() || !bookingRows.findAll().isEmpty() || !staffRows.findAll().isEmpty()) return;
        for (String username : new String[]{"manager","cashier","mechanic1","mechanic2","mechanic3"}) {
            AppUser account=users.getUserByUsername(username).orElseThrow();
            Staff employee=new Staff(); employee.setName(account.getFullName()); employee.setRole(account.getRole()); employee.setPhone(account.getPhoneNumber()); employee.setEmail(account.getEmail()); employee.setAssignedStation("FuelCore Main Station"); employee.setSystemUsername(username); employee=hr.saveStaff(employee);
            ShiftSchedule shift=new ShiftSchedule();shift.setStaffId(employee.getId());shift.setShiftDate(LocalDate.now(clock).plusDays(1));shift.setShiftTiming("Day (08:00 - 18:00)");shift.setStation(employee.getAssignedStation());shift.setStatus("Scheduled");hr.saveShift(shift);
        }
        for(int i=1;i<=3;i++){ServiceBay bay=new ServiceBay();bay.setBayName("Service Bay "+i);bay.setStatus("Available");workshop.addBay(bay);}
        service("Full Service",12000,"2 Hours","Maintenance"); service("Repair",15000,"3 Hours","Repair");service("Oil Change",4500,"1 Hour","Maintenance");service("Vehicle Inspection",2500,"30 Minutes","Inspection");
        Supplier supplier=new Supplier();supplier.setSupplierName("Demo Parts Supplier");supplier.setCompany("FuelCore Test Supply");supplier.setContactNumber("0771234567");supplier.setEmail("supplier@example.invalid");billing.saveSupplier(supplier);
        SparePart filter=part("Oil Filter","Filters",500,100,"Demo Parts Supplier");part("Brake Pads","Brakes",4500,40,"Demo Parts Supplier");part("Engine Oil 1L","Lubricants",2500,80,"Demo Parts Supplier");
        Offer offer=new Offer();offer.setTargetType(OfferTargetType.SPARE_PART);offer.setTargetId(filter.getId());offer.setDiscountPercentage(10.0);offer.setActive(true);offers.saveOffer(offer);
        vehicle("ABC-1234","customer1","Kasun Dias","Toyota","Corolla");vehicle("CAR-2026","customer2","Nuwan Kulasekara","Nissan","Leaf");vehicle("REG-2222",null,"Registered Vehicle Owner","Honda","Civic");
        ServiceBooking account=book("ABC-1234","Full Service","09:00 AM");
        billing.checkoutService(account.getReferenceNumber(),0.0,"CARD",5000.0,null,"demo-service-advance");
        Staff mechanic=workshop.getAvailableMechanics().get(0);workshop.createJobCard(account.getReferenceNumber(),mechanic.getId(),workshop.getAllBays().get(0).getId());
        book("REG-2222","Oil Change","11:00 AM");book("GUEST-3333","Repair","01:00 PM");
        billing.purchasePart(filter.getId(),1,"customer1",null,null,"CARD",null,"demo-account-parts");
        billing.purchasePart(filter.getId(),1,null,"REG-2222",null,"CASH",1000.0,"demo-vehicle-parts");
        billing.purchasePart(filter.getId(),1,null,null,"Guest Parts Buyer","CARD",null,"demo-guest-parts");
        fuel.refreshStationDay();
        var pump=fuel.getAllPumps().get(0);fuel.recordDailyFigures(pump.getId(),"Active",null,25.0,25*fuel.getAllPrices().stream().filter(p->p.getFuelType().equals(pump.getFuelType())).findFirst().orElseThrow().getCurrentPrice(),true);
    }
    private void service(String name,double cost,String duration,String category){ServiceCatalogItem s=new ServiceCatalogItem();s.setName(name);s.setEstimatedCost(cost);s.setEstimatedDuration(duration);s.setCategory(category);s.setIcon("fa-wrench");s.setShortDescription(name+" package. Parts are billed separately when used.");s.setActive(true);catalog.saveService(s);}
    private SparePart part(String name,String category,double price,int stock,String supplier){SparePart p=new SparePart();p.setPartName(name);p.setCategory(category);p.setSellingPrice(price);p.setBuyingPrice(price*.7);p.setStockQuantity(stock);p.setMinStockWarning(5);p.setSupplier(supplier);return billing.saveSparePart(p);}
    private void vehicle(String plate,String username,String name,String make,String model){Vehicle v=new Vehicle();v.setLicensePlate(plate);v.setOwnerUsername(username);v.setOwnerName(name);v.setOwnerContact("0771234567");v.setMake(make);v.setModel(model);v.setManufactureYear(2020);v.setFuelType("Petrol");v.setMileage(50000);vehicles.saveVehicle(v);}
    private ServiceBooking book(String plate,String service,String slot){ServiceBooking b=new ServiceBooking();b.setLicensePlate(plate);b.setCustomerName("Guest Service Customer");b.setCustomerPhone("0771234567");b.setServiceType(service);b.setServiceDate(LocalDate.now(clock).plusDays(1));b.setTimeSlot(slot);return bookings.createBooking(b);}
}
