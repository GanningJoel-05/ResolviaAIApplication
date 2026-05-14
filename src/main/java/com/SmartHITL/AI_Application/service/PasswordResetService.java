package com.SmartHITL.AI_Application.service;

import com.SmartHITL.AI_Application.entity.User;
import com.SmartHITL.AI_Application.repository.PasswordResetTokenRepository;
import com.SmartHITL.AI_Application.repository.UserRepository;
import com.SmartHITL.AI_Application.token.PasswordResetToken;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository                userRepository;
    private final PasswordResetTokenRepository  tokenRepository;
    private final PasswordEncoder               passwordEncoder;
    private final EmailService                  emailService;

    /**
     * Check the email exists, create a reset token, send the reset email.
     * Returns the generated token (for testing/logging only — not sent to client).
     */
    public String initiatePasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("EMAIL_NOT_FOUND"));

        // Block password reset for unverified accounts
        if (!user.isEmailVerified()) {
            throw new RuntimeException("EMAIL_NOT_VERIFIED");
        }

        // Invalidate any previous unused tokens for this user
        tokenRepository.findAll().stream()
                .filter(t -> t.getUser().getId().equals(user.getId()) && !t.isUsed())
                .forEach(t -> {
                    t.setUsed(true);
                    tokenRepository.save(t);
                });

        String token = UUID.randomUUID().toString();

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiryDate(LocalDateTime.now().plusMinutes(15))
                .used(false)
                .createdAt(LocalDateTime.now())
                .build();

        tokenRepository.save(resetToken);

        // Send the reset email with a link
        emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), token);

        return token;
    }

    /**
     * Validate a reset token without consuming it.
     * Throws descriptive RuntimeException if invalid, expired, or already used.
     */
    public void validateResetToken(String token) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid reset link. Please request a new one."));
        if (resetToken.isUsed()) {
            throw new RuntimeException("ALREADY_USED: This reset link has already been used. Please request a new one.");
        }
        if (LocalDateTime.now().isAfter(resetToken.getExpiryDate())) {
            throw new RuntimeException("EXPIRED: This reset link has expired. Please request a new one.");
        }
        // Token is valid — do nothing (don't consume it)
    }

    /**
     */
    public void resetPasswordWithToken(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid or expired reset link. Please request a new one."));

        if (resetToken.isUsed()) {
            throw new RuntimeException("This reset link has already been used. Please request a new one.");
        }
        if (LocalDateTime.now().isAfter(resetToken.getExpiryDate())) {
            throw new RuntimeException("This reset link has expired (15 min limit). Please request a new one.");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
    }

    /**
     * Legacy: reset by email directly (kept for internal use / admin flows).
     */
    public void resetPassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public boolean checkEmailExists(String email) {
        return userRepository.findByEmail(email).isPresent();
    }
}