package com.fuelstation.service;

import com.fuelstation.model.AppUser;
import com.fuelstation.repository.AppUserRepository;
import com.fuelstation.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserPasswordValidationTest {
    @Mock AppUserRepository users;
    @Mock PasswordEncoder encoder;
    @Mock AuditLogService audit;
    @Mock NotificationService notifications;
    @InjectMocks UserServiceImpl service;
    private AppUser user(String password){
        var user=new AppUser();user.setUsername("test.customer");user.setFullName("Test Customer");user.setEmail("test@example.com");user.setRole("Customer");user.setPassword(password);return user;
    }
    @Test void accountCreationRejectsSevenCharactersBeforeHashingOrSaving(){
        assertThrows(IllegalArgumentException.class,()->service.createUser(user("Abcd123")));
        verifyNoInteractions(encoder,audit,notifications);verify(users,never()).save(any());
    }
    @Test void accountCreationAcceptsEightCharactersAndStoresTheHash(){
        when(encoder.encode("Abcd1234")).thenReturn("hashed-password");when(users.save(any())).thenAnswer(call->call.getArgument(0));
        assertEquals("hashed-password",service.createUser(user("Abcd1234")).getPassword());verify(users).save(any());
    }
}
