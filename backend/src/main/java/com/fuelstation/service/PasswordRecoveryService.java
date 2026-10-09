package com.fuelstation.service;

import com.fuelstation.repository.AppUserRepository;
import com.fuelstation.util.Rules;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PasswordRecoveryService {
    private final AppUserRepository accounts;
    private final UserService users;
    private final ObjectProvider<JavaMailSender> sender;
    private final Clock clock;
    private final String from;
    private final String frontendUrl;
    private final boolean enabled;
    private final ConcurrentHashMap<String, Instant> recent = new ConcurrentHashMap<>();
    public PasswordRecoveryService(AppUserRepository accounts, UserService users, ObjectProvider<JavaMailSender> sender, Clock clock,
            @Value("${app.mail.from:}") String from, @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl,
            @Value("${app.mail.enabled:false}") boolean enabled) {
        this.accounts = accounts; this.users = users; this.sender = sender; this.clock = clock;
        this.from = from; this.frontendUrl = frontendUrl; this.enabled = enabled;
    }
    @Transactional
    public synchronized void request(String identifier) {
        String clean = Rules.required(identifier, "Username or email").toLowerCase(java.util.Locale.ROOT);
        JavaMailSender mail = sender.getIfAvailable();
        if (!enabled || mail == null || from.isBlank()) throw new IllegalStateException("Password recovery email is not configured. Please contact the station administrator.");
        Instant now = clock.instant();
        if (recent.size() > 10000) recent.entrySet().removeIf(e -> e.getValue().isBefore(now.minusSeconds(60)));
        Instant last = recent.put(clean, now);
        if (last != null && last.isAfter(now.minusSeconds(60))) return;
        var user = accounts.findByUsernameIgnoreCase(clean).or(() -> accounts.findByEmailIgnoreCase(clean));
        if (user.isEmpty() || !"Active".equalsIgnoreCase(user.get().getStatus())) return;
        String token = users.generatePasswordResetToken(user.get().getUsername());
        if (token == null || user.get().getEmail() == null || user.get().getEmail().isBlank()) return;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from); message.setTo(user.get().getEmail()); message.setSubject("Reset your FuelCore password");
        message.setText("A password reset was requested for your FuelCore account.\n\nOpen this link within 30 minutes:\n" + frontendUrl.replaceAll("/$", "") + "/reset-password?token=" + token + "\n\nIf you did not request this, you can ignore this email.");
        try { mail.send(message); } catch (org.springframework.mail.MailException e) { recent.remove(clean); throw new IllegalStateException("Recovery email could not be sent. Please try again later."); }
    }
}
