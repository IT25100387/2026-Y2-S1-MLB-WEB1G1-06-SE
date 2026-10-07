package com.fuelstation.config;

import com.fuelstation.model.AppUser;
import com.fuelstation.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;



@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="app.demo.seed", havingValue="true")
@Order(1)
public class SecurityDataInitializer implements CommandLineRunner {

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired private com.fuelstation.service.UserService users;

    @Autowired
    private com.fuelstation.service.FuelService fuelService;

    @Override
    public void run(String... args) {
        // Ensure default users exist with properly BCrypt-hashed passwords
        createOrUpdateUser("admin", "Admin@123", "Milinda Kalum", "admin@fuelcore.com", "Admin");
        createOrUpdateUser("manager", "manager@123", "Sunil Perera", "manager@fuelcore.com", "Manager");
        createOrUpdateUser("cashier", "cashier@123", "Kamal Fernando", "cashier@fuelcore.com", "Cashier");
        createOrUpdateUser("mechanic1", "mechanic1@123", "Nimal Jayawardena", "mechanic@fuelcore.com", "Mechanic");
        createOrUpdateUser("mechanic2", "mechanic2@123", "Kamal Silva", "kamal.silva@fuelcore.com", "Mechanic");
        createOrUpdateUser("mechanic3", "mechanic3@123", "Ruwan Fernando", "ruwan.fernando@fuelcore.com", "Mechanic");
        createOrUpdateUser("customer1", "customer1@123", "Kasun Dias", "kasun@gmail.com", "Customer");
        createOrUpdateUser("customer2", "customer2@123", "Nuwan Kulasekara", "nuwan@gmail.com", "Customer");

        // Ensure default fuel tariffs and initial forecourt data
        fuelService.seedDefaultFuelDataIfEmpty();
    }

    private void createOrUpdateUser(String username, String rawPassword, String fullName, String email, String role) {
        if (userRepository.findByUsername(username).isPresent()) return;
        AppUser user = new AppUser(); user.setUsername(username); user.setPassword(rawPassword); user.setFullName(fullName); user.setEmail(email); user.setRole(role); user.setPhoneNumber("0771234567"); users.createUser(user);
    }
}
