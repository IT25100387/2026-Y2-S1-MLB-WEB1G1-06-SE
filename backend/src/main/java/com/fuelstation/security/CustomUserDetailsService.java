package com.fuelstation.security;

import com.fuelstation.model.AppUser;
import com.fuelstation.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private AppUserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null || username.trim().isEmpty()) {
            throw new UsernameNotFoundException("Username cannot be empty");
        }

        String cleanUsername = username.trim();
        AppUser appUser = userRepository.findByUsername(cleanUsername)
                .or(() -> userRepository.findByUsernameIgnoreCase(cleanUsername))
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + cleanUsername));

        boolean enabled = "Active".equalsIgnoreCase(appUser.getStatus());

        List<GrantedAuthority> authorities = new ArrayList<>();
        String roleStr = appUser.getRole();
        if (roleStr != null && !roleStr.trim().isEmpty()) {
            String roleUpper = roleStr.trim().toUpperCase();
            if (!roleUpper.startsWith("ROLE_")) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + roleUpper));
            } else {
                authorities.add(new SimpleGrantedAuthority(roleUpper));
            }
            authorities.add(new SimpleGrantedAuthority(roleStr.trim()));
        }

        String storedPassword = appUser.getPassword() != null ? appUser.getPassword().trim() : "";

        return new User(
                appUser.getUsername(),
                storedPassword,
                enabled,
                true, // accountNonExpired
                true, // credentialsNonExpired
                true, // accountNonLocked
                authorities
        );
    }
}
