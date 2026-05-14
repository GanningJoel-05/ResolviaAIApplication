package com.SmartHITL.AI_Application.service;

import com.SmartHITL.AI_Application.dto.AuthResponseDTO;
import com.SmartHITL.AI_Application.dto.LoginRequestDTO;
import com.SmartHITL.AI_Application.dto.RegisterRequestDTO;
import com.SmartHITL.AI_Application.entity.EmailVerificationToken;
import com.SmartHITL.AI_Application.entity.Role;
import com.SmartHITL.AI_Application.entity.User;
import com.SmartHITL.AI_Application.repository.EmailVerificationTokenRepository;
import com.SmartHITL.AI_Application.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository                   userRepository;
    private final BCryptPasswordEncoder            passwordEncoder;
    private final JwtService                       jwtService;
    private final EmailService                     emailService;
    private final EmailVerificationTokenRepository verificationTokenRepository;

    // ── REGISTER ──────────────────────────────────────────────────────────
    @Transactional
    public AuthResponseDTO register(RegisterRequestDTO request) {

        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new RuntimeException("Email already registered");
        }

        // Validate username
        String usernameClean = (request.username() != null)
                ? request.username().toLowerCase().trim() : null;
        if (usernameClean == null || usernameClean.isBlank()) {
            throw new RuntimeException("Username is required.");
        }
        if (!usernameClean.matches("^[a-z0-9_]{3,20}$")) {
            throw new RuntimeException(
                    "Username must be 3-20 characters: lowercase letters, numbers, underscores only.");
        }
        if (userRepository.findByUsername(usernameClean).isPresent()) {
            throw new RuntimeException("Username already taken. Please choose a different one.");
        }

        String email         = request.email();
        String requestedRole = request.role().toUpperCase();

        // Typo / fake domain check
        String emailDomain = email.substring(email.indexOf("@") + 1).toLowerCase();
        if (emailDomain.equals("ggmail.com") || emailDomain.equals("gamil.com") ||
                emailDomain.equals("gmaill.com") || emailDomain.equals("gmail.co") ||
                emailDomain.startsWith("gggg")) {
            throw new RuntimeException("Invalid email domain. Please enter a valid email address.");
        }

        if (requestedRole.equals("ADMIN") && !email.endsWith("@psnacet.edu.in")) {
            throw new RuntimeException("Only @psnacet.edu.in email addresses can register as ADMIN");
        }

        Role role = Role.valueOf(requestedRole);

        // Create user — emailVerified = false until they click the link
        User user = User.builder()
                .name(request.name())
                .username(usernameClean)
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .role(role)
                .accountStatus("ACTIVE")
                .emailVerified(false)
                .build();

        userRepository.save(user);

        // Generate verification token (valid 24 hours)
        String token = UUID.randomUUID().toString();
        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .token(token)
                .user(user)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .used(false)
                .build();
        verificationTokenRepository.save(verificationToken);

        // Send verification email
        emailService.sendVerificationEmail(user.getEmail(), user.getName(), token);

        // Do NOT return a JWT yet — user must verify email first
        return new AuthResponseDTO(null, role.name(),
                "Registration successful! Please check your email and verify your account before logging in.");
    }

    // ── LOGIN ─────────────────────────────────────────────────────────────
    public AuthResponseDTO login(LoginRequestDTO request) {

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        // Block unverified accounts from logging in
        if (!user.isEmailVerified()) {
            throw new RuntimeException(
                    "EMAIL_NOT_VERIFIED: Please verify your email address before logging in. " +
                            "Check your inbox for the verification link.");
        }

        // Auto-restore expired temporary blocks
        String status = user.getAccountStatus();
        if ("TEMPORARY".equals(status)) {
            LocalDateTime expires = user.getBlockExpiresAt();
            if (expires != null && LocalDateTime.now().isAfter(expires)) {
                user.setAccountStatus("ACTIVE");
                user.setBlockReason(null);
                user.setBlockedAt(null);
                user.setBlockExpiresAt(null);
                user.setBlockedByAdminId(null);
                userRepository.save(user);
            }
        }

        String token = jwtService.generateToken(user.getEmail(), user.getRole().name());
        return new AuthResponseDTO(token, user.getRole().name(), "Login successful");
    }
}