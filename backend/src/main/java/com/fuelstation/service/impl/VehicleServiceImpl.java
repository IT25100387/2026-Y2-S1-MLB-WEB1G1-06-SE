package com.fuelstation.service.impl;

import com.fuelstation.model.ServiceBooking;
import com.fuelstation.model.Vehicle;
import com.fuelstation.repository.ServiceBookingRepository;
import com.fuelstation.repository.VehicleRepository;
import com.fuelstation.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class VehicleServiceImpl implements VehicleService {

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private ServiceBookingRepository bookingRepository;

    @Autowired private com.fuelstation.repository.AppUserRepository accounts;
    @Autowired private com.fuelstation.repository.InvoiceRepository invoices;

    @Override
    @Transactional(readOnly = true)
    public List<Vehicle> getAllVehicles() {
        return vehicleRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Vehicle> getVehicleById(Long id) {
        return vehicleRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Vehicle> getVehicleByLicensePlate(String licensePlate) {
        if (licensePlate == null || licensePlate.trim().isEmpty()) {
            return Optional.empty();
        }
        return vehicleRepository.findByLicensePlate(licensePlate.trim());
    }

    @Override
    @Transactional
    public Vehicle saveVehicle(Vehicle vehicle) {
        if (vehicle.getLicensePlate() == null || vehicle.getLicensePlate().trim().isEmpty() ||
            vehicle.getMake() == null || vehicle.getMake().trim().isEmpty() ||
            vehicle.getModel() == null || vehicle.getModel().trim().isEmpty() ||
            vehicle.getOwnerName() == null || vehicle.getOwnerName().trim().isEmpty() ||
            vehicle.getOwnerContact() == null || vehicle.getOwnerContact().trim().isEmpty()) {
            throw new IllegalArgumentException("License Plate Number, Vehicle Make / Brand, Vehicle Model, Registered Owner Name, and Owner Contact Phone are mandatory fields.");
        }
        vehicle.setLicensePlate(vehicle.getLicensePlate().trim().toUpperCase(java.util.Locale.ROOT));
        vehicle.setMake(vehicle.getMake().trim()); vehicle.setModel(vehicle.getModel().trim()); vehicle.setOwnerName(vehicle.getOwnerName().trim()); vehicle.setOwnerContact(vehicle.getOwnerContact().trim());
        var duplicate = vehicleRepository.findByLicensePlate(vehicle.getLicensePlate());
        if (duplicate.isPresent() && !java.util.Objects.equals(duplicate.get().getId(), vehicle.getId())) throw new IllegalArgumentException("License plate already registered");
        if (vehicle.getManufactureYear() != null && (vehicle.getManufactureYear()<1900 || vehicle.getManufactureYear()>java.time.LocalDate.now(java.time.ZoneId.of("Asia/Colombo")).getYear()+1)) throw new IllegalArgumentException("Enter a valid manufacture year");
        if (vehicle.getOwnerUsername()!=null && !vehicle.getOwnerUsername().isBlank()) {
            var owner=accounts.findByUsername(vehicle.getOwnerUsername()).filter(a -> "Customer".equals(a.getRole())).orElseThrow(() -> new IllegalArgumentException("Select a customer account")); vehicle.setOwnerUsername(owner.getUsername());
        } else vehicle.setOwnerUsername(null);
        if (vehicle.getId()!=null) { Vehicle old=vehicleRepository.findById(vehicle.getId()).orElseThrow(() -> new IllegalArgumentException("Vehicle not found")); if (!old.getLicensePlate().equals(vehicle.getLicensePlate()) && !bookingRepository.findByLicensePlateOrderByServiceDateDesc(old.getLicensePlate()).isEmpty()) throw new IllegalStateException("This plate has service history. Retain it to preserve those links."); }
        if (vehicle.getRegisteredDate() == null || vehicle.getRegisteredDate().trim().isEmpty()) {
            vehicle.setRegisteredDate(LocalDate.now().toString());
        }
        return vehicleRepository.save(vehicle);
    }

    @Override
    @Transactional
    public void deleteVehicle(Long id) {
        Vehicle vehicle=vehicleRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));
        if (!bookingRepository.findByLicensePlateOrderByServiceDateDesc(vehicle.getLicensePlate()).isEmpty() || invoices.findAll().stream().anyMatch(i -> vehicle.getLicensePlate().equals(i.getLicensePlate()))) throw new IllegalStateException("Vehicle has service or invoice history. Retain the vehicle to preserve those links.");
        vehicleRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceBooking> getServiceHistory(String licensePlate) {
        if (licensePlate == null || licensePlate.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return bookingRepository.findByLicensePlateOrderByServiceDateDesc(licensePlate.trim());
    }

    // --- Phase 2: Customer Vehicle Management (My Garage) ---

    @Override
    @Transactional(readOnly = true)
    public List<Vehicle> getVehiclesByOwner(String username) {
        if (username == null || username.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String cleanUser = username.trim();
        return vehicleRepository.findByOwnerUsernameIgnoreCase(cleanUser);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Vehicle> getVehicleForCustomer(Long id, String username) {
        if (id == null || username == null || username.trim().isEmpty()) {
            return Optional.empty();
        }
        Optional<Vehicle> opt = vehicleRepository.findById(id);
        if (opt.isPresent()) {
            Vehicle v = opt.get();
            if (isOwnedBy(v, username)) {
                return opt;
            }
        }
        return Optional.empty();
    }

    @Override
    @Transactional
    public Vehicle saveCustomerVehicle(Vehicle vehicle, String username, String ownerName, String ownerContact) {
        vehicle.setId(null);
        vehicle.setOwnerUsername(username);
        if (vehicle.getOwnerName() == null || vehicle.getOwnerName().trim().isEmpty()) {
            vehicle.setOwnerName(ownerName != null ? ownerName : username);
        }
        if (vehicle.getOwnerContact() == null || vehicle.getOwnerContact().trim().isEmpty()) {
            if (ownerContact != null && !ownerContact.trim().isEmpty()) {
                vehicle.setOwnerContact(ownerContact.trim());
            } else {
                vehicle.setOwnerContact("Customer");
            }
        }
        if (vehicle.getRegisteredDate() == null || vehicle.getRegisteredDate().trim().isEmpty()) {
            vehicle.setRegisteredDate(LocalDate.now().toString());
        }
        return saveVehicle(vehicle);
    }

    @Override
    @Transactional
    public boolean deleteCustomerVehicle(Long id, String username) {
        Optional<Vehicle> opt = getVehicleForCustomer(id, username);
        if (opt.isPresent()) {
            deleteVehicle(id);
            return true;
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isVehicleOwnedByUser(Long id, String username) {
        return getVehicleForCustomer(id, username).isPresent();
    }

    private boolean isOwnedBy(Vehicle v, String username) {
        if (v == null || username == null) return false;
        String cleanUser = username.trim();
        if (v.getOwnerUsername() != null && v.getOwnerUsername().equalsIgnoreCase(cleanUser)) {
            return true;
        }

        return false;
    }
}
