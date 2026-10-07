package com.fuelstation.service;

import com.fuelstation.model.AppUser;

import java.util.List;
import java.util.Optional;

public interface UserService {
    List<AppUser> getAllUsers();
    Optional<AppUser> getUserById(Long id);
    Optional<AppUser> getUserByUsername(String username);
    AppUser registerOrUpdateUser(AppUser user);
    AppUser createUser(AppUser user);
    AppUser updateUser(Long id, AppUser updatedUser, String newPassword);
    void deactivateUser(Long id);
    void reactivateUser(Long id);
    void toggleUserStatus(Long id);
    void deleteUser(Long id);
    List<AppUser> searchUsers(String query, String role, String status);

    // Profile & Security (Module 1)
    AppUser updateProfile(String username, String fullName, String email, String phone, String address, String city);
    AppUser updateSettings(String username, boolean emailNotif, boolean smsNotif);
    boolean changePassword(String username, String currentPassword, String newPassword);
    String generatePasswordResetToken(String identifier);
    boolean resetPasswordWithToken(String token, String newPassword);
    List<AppUser> getCustomersOnly();
}
