package com.fuelstation.service.impl;

import com.fuelstation.model.AppUser;
import com.fuelstation.repository.AppUserRepository;
import com.fuelstation.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {
    @Autowired private java.time.Clock clock;
    private String tokenHash(String token) { try { return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8))); } catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); } }

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private com.fuelstation.service.AuditLogService auditLogService;
    @Autowired private com.fuelstation.service.NotificationService notifications;
    @Autowired private com.fuelstation.repository.StaffRepository staffRepository;
    @Autowired private com.fuelstation.repository.JobCardRepository jobRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AppUser> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AppUser> getUserById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AppUser> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    @Transactional
    public AppUser registerOrUpdateUser(AppUser user) {
        return user.getId() == null ? createUser(user) : updateUser(user.getId(), user, user.getPassword());
    }

    @Override
    @Transactional
    public AppUser createUser(AppUser user) {
        String username = com.fuelstation.util.Rules.required(user.getUsername(), "Username");
        com.fuelstation.util.InputValidation.username(username,true);
        if (userRepository.existsByUsernameIgnoreCase(username)) throw new com.fuelstation.util.InputValidation.InvalidFields(java.util.Map.of("username","Username already exists"));
        user.setEmail(validEmail(user.getEmail(), null));
        user.setFullName(com.fuelstation.util.InputValidation.name(user.getFullName(), "Full name",true));
        user.setPhoneNumber(com.fuelstation.util.InputValidation.phone(user.getPhoneNumber(),"Phone",false));
        if (user.getPassword() == null || user.getPassword().length() < 6) throw new IllegalArgumentException("Password must have at least 6 characters");
        if (!java.util.List.of("Admin", "Manager", "Cashier", "Mechanic", "Customer").contains(user.getRole())) throw new IllegalArgumentException("Select a valid account role");
        user.setId(null); user.setUsername(username); user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setStatus("Active"); user.setCreatedAt(java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Colombo"))); user.setResetToken(null); user.setResetTokenExpiry(null);
        AppUser saved = userRepository.save(user);
        auditLogService.record("USER_CREATED", saved.getUsername(), saved.getRole(), "Account created", "SUCCESS");
        notifications.notifyRoles("Account created", saved.getUsername() + " | " + saved.getRole(), "Account", "/dashboard/users", "ADMIN");
        notifications.createCustomerNotification(saved.getUsername(), "Welcome to FuelCore", "Your account is ready. Use the notification bell for your updates.", "Account", null, "fa-user");
        return saved;
    }

    private String validEmail(String value, Long excludedId) {
        String email = com.fuelstation.util.Rules.required(value, "Email").toLowerCase(java.util.Locale.ROOT);
        com.fuelstation.util.InputValidation.email(email,true);
        userRepository.findByEmailIgnoreCase(email).filter(u -> !java.util.Objects.equals(u.getId(), excludedId)).ifPresent(u -> { throw new com.fuelstation.util.InputValidation.InvalidFields(java.util.Map.of("email","Email address already belongs to another account")); });
        return email;
    }

    @Override
    @Transactional
    public AppUser updateUser(Long id, AppUser updatedUser, String newPassword) {
        AppUser existing = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));

        var linkedStaff = staffRepository.findBySystemUsername(existing.getUsername());
        if (linkedStaff.isPresent()) {
            var staff = linkedStaff.get();
            if (staff.getRole()!=null && staff.getRole().toLowerCase(java.util.Locale.ROOT).contains("mechanic") && updatedUser.getRole()!=null && !updatedUser.getRole().isBlank() && !"Mechanic".equals(updatedUser.getRole())) throw new IllegalStateException("Change the linked employee designation before changing this Mechanic account role.");
            if (updatedUser.getStatus()!=null && !"Active".equals(updatedUser.getStatus()) && jobRepository.findByMechanicId(staff.getId()).stream().anyMatch(j -> !java.util.Set.of("Completed","Cancelled").contains(j.getStatus()))) throw new IllegalStateException("This account has assigned service jobs. Complete or update their workshop allocations before deactivating it.");
        }

        if (updatedUser.getFullName() != null && !updatedUser.getFullName().isBlank()) {
            existing.setFullName(com.fuelstation.util.InputValidation.name(updatedUser.getFullName(),"Full name",true));
        }
        if (updatedUser.getEmail() != null && !updatedUser.getEmail().isBlank()) {
            existing.setEmail(validEmail(updatedUser.getEmail(), id));
        }
        if (updatedUser.getPhoneNumber() != null) {
            existing.setPhoneNumber(com.fuelstation.util.InputValidation.phone(updatedUser.getPhoneNumber(),"Phone",false));
        }
        if (updatedUser.getAddress() != null) {
            existing.setAddress(com.fuelstation.util.InputValidation.text(updatedUser.getAddress(),"Address",255,false,false));
        }
        if (updatedUser.getCity() != null) {
            existing.setCity(com.fuelstation.util.InputValidation.text(updatedUser.getCity(),"City",80,false,false));
        }
        if (updatedUser.getRole() != null && !updatedUser.getRole().isBlank()) {
            if (!java.util.List.of("Admin", "Manager", "Cashier", "Mechanic", "Customer").contains(updatedUser.getRole())) throw new IllegalArgumentException("Select a valid account role");
            if ("admin".equalsIgnoreCase(existing.getUsername()) && !"Admin".equals(updatedUser.getRole())) throw new IllegalArgumentException("The root administrator role is protected");
            existing.setRole(updatedUser.getRole().trim());
        }
        if (updatedUser.getStatus() != null && !updatedUser.getStatus().isBlank()) {
            if (!java.util.List.of("Active", "Deactivated").contains(updatedUser.getStatus())) throw new IllegalArgumentException("Select a valid account status");
            if ("admin".equalsIgnoreCase(existing.getUsername()) && !"Active".equals(updatedUser.getStatus())) throw new IllegalArgumentException("The root administrator cannot be deactivated");
            existing.setStatus(updatedUser.getStatus().trim());
        }

        if (newPassword != null && !newPassword.trim().isEmpty()) {
            com.fuelstation.util.InputValidation.password(newPassword,true);
            existing.setPassword(passwordEncoder.encode(newPassword));
        }

        AppUser saved = userRepository.save(existing);
        try {
            auditLogService.record("USER_UPDATED", saved.getUsername(), saved.getRole(),
                    "Updated profile details for " + saved.getFullName() + " (Status: " + saved.getStatus() + ")", "SUCCESS");
        } catch (Exception ignored) {}
        return saved;
    }

    @Override
    @Transactional
    public void deactivateUser(Long id) {
        AppUser change = new AppUser(); change.setStatus("Deactivated");
        updateUser(id, change, null);
    }

    @Override
    @Transactional
    public void reactivateUser(Long id) {
        AppUser change = new AppUser(); change.setStatus("Active");
        updateUser(id, change, null);
    }

    @Override
    @Transactional
    public void toggleUserStatus(Long id) {
        AppUser existing = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
        AppUser change = new AppUser(); change.setStatus("Active".equals(existing.getStatus()) ? "Deactivated" : "Active");
        updateUser(id, change, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppUser> searchUsers(String query, String role, String status) {
        String cleanQuery = (query != null) ? query.trim().toLowerCase() : "";
        String cleanRole = (role != null) ? role.trim() : "ALL";
        String cleanStatus = (status != null) ? status.trim() : "ALL";

        return userRepository.findAll().stream().filter(u -> {
            boolean matchesQuery = cleanQuery.isEmpty() ||
                    (u.getFullName() != null && u.getFullName().toLowerCase().contains(cleanQuery)) ||
                    (u.getUsername() != null && u.getUsername().toLowerCase().contains(cleanQuery)) ||
                    (u.getEmail() != null && u.getEmail().toLowerCase().contains(cleanQuery));

            boolean matchesRole = cleanRole.equalsIgnoreCase("ALL") ||
                    (u.getRole() != null && u.getRole().equalsIgnoreCase(cleanRole));

            boolean matchesStatus = cleanStatus.equalsIgnoreCase("ALL") ||
                    (u.getStatus() != null && u.getStatus().equalsIgnoreCase(cleanStatus));

            return matchesQuery && matchesRole && matchesStatus;
        }).toList();
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        deactivateUser(id);
    }

    @Override
    @Transactional
    public AppUser updateProfile(String username, String fullName, String email, String phone, String address, String city) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        user.setFullName(com.fuelstation.util.InputValidation.name(fullName,"Full name",true));
        user.setEmail(validEmail(email, user.getId()));
        user.setPhoneNumber(com.fuelstation.util.InputValidation.phone(phone,"Phone number",false));
        user.setAddress(com.fuelstation.util.InputValidation.text(address,"Address",255,false,false));
        user.setCity(com.fuelstation.util.InputValidation.text(city,"City",80,false,false));

        return userRepository.save(user);
    }

    @Override
    @Transactional
    public AppUser updateSettings(String username, boolean emailNotif, boolean smsNotif) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        user.setEmailNotifications(emailNotif);
        user.setSmsNotifications(smsNotif);
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public boolean changePassword(String username, String currentPassword, String newPassword) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        com.fuelstation.util.InputValidation.password(newPassword,true);
        if(java.util.Objects.equals(currentPassword,newPassword))throw new IllegalArgumentException("Choose a password different from your current password");
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPassword())) {
            return false;
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        notifications.createCustomerNotification(username, "Password changed", "Your account password was updated.", "Account", null, "fa-shield-halved");
        try {
            auditLogService.record("PASSWORD_CHANGED", username, user.getRole(),
                    "User updated account password via BCrypt verification", "SUCCESS");
        } catch (Exception ignored) {}
        return true;
    }

    @Override
    @Transactional
    public String generatePasswordResetToken(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) return null;
        String clean = identifier.trim();

        Optional<AppUser> userOpt = userRepository.findByEmailIgnoreCase(clean);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByUsernameIgnoreCase(clean);
        }

        if (userOpt.isPresent()) {
            AppUser user = userOpt.get();
            String token = java.util.UUID.randomUUID().toString();
            user.setResetToken(tokenHash(token));
            user.setResetTokenExpiry(java.time.LocalDateTime.now(clock).plusMinutes(30));
            userRepository.save(user);
            try {
                auditLogService.record("PASSWORD_RESET_REQUESTED", user.getUsername(), user.getRole(),
                        "Generated password reset token for account recovery", "SUCCESS");
            } catch (Exception ignored) {}
            return token;
        }
        return null;
    }

    @Override
    @Transactional
    public boolean resetPasswordWithToken(String token, String newPassword) {
        com.fuelstation.util.InputValidation.password(newPassword,true);
        if (token == null || token.trim().isEmpty()) return false;
        Optional<AppUser> userOpt = userRepository.findByResetToken(tokenHash(token.trim()));
        if (userOpt.isEmpty()) return false;

        AppUser user = userOpt.get();
        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(java.time.LocalDateTime.now(clock))) {
            return false;
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
        try {
            auditLogService.record("PASSWORD_RESET_SUCCESS", user.getUsername(), user.getRole(),
                    "Account password successfully reset via token authorization", "SUCCESS");
        } catch (Exception ignored) {}
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppUser> getCustomersOnly() {
        return userRepository.findAll().stream()
                .filter(u -> "Customer".equalsIgnoreCase(u.getRole()) || "ROLE_CUSTOMER".equalsIgnoreCase(u.getRole()))
                .toList();
    }
}
