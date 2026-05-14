package com.SmartHITL.AI_Application.controller;

import com.SmartHITL.AI_Application.entity.EmailVerificationToken;
import com.SmartHITL.AI_Application.entity.User;
import com.SmartHITL.AI_Application.repository.EmailVerificationTokenRepository;
import com.SmartHITL.AI_Application.repository.UserRepository;
import com.SmartHITL.AI_Application.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class EmailVerificationController {

    private final EmailVerificationTokenRepository verificationTokenRepository;
    private final UserRepository                   userRepository;
    private final EmailService                     emailService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    // ── GET /api/auth/check-username?username=joel_2005 ──────────────────
    // Public endpoint — called live from register.html as user types.
    // Returns { "available": true } or { "available": false }
    @GetMapping("/check-username")
    public ResponseEntity<Map<String, Object>> checkUsername(@RequestParam String username) {
        if (username == null || username.isBlank()) {
            return ResponseEntity.ok(Map.of("available", false, "reason", "empty"));
        }
        String clean = username.toLowerCase().trim();
        if (!clean.matches("^[a-z0-9_]{3,20}$")) {
            return ResponseEntity.ok(Map.of("available", false, "reason", "invalid_format"));
        }
        boolean taken = userRepository.findByUsername(clean).isPresent();
        return ResponseEntity.ok(Map.of("available", !taken));
    }

    // ── GET /api/auth/verify-email?token=... ─────────────────────────────
    // Called when user clicks the link in their email.
    // Redirects to email-verification.html with result param.
    @GetMapping("/verify-email")
    public void verifyEmail(@RequestParam String token,
                            jakarta.servlet.http.HttpServletResponse response)
            throws java.io.IOException {

        EmailVerificationToken vToken = verificationTokenRepository
                .findByToken(token).orElse(null);

        if (vToken == null) {
            response.sendRedirect(frontendUrl + "/email-verification.html?status=invalid");
            return;
        }
        if (vToken.isUsed()) {
            response.sendRedirect(frontendUrl + "/email-verification.html?status=already_used");
            return;
        }
        if (LocalDateTime.now().isAfter(vToken.getExpiresAt())) {
            response.sendRedirect(frontendUrl + "/email-verification.html?status=expired&email="
                    + vToken.getUser().getEmail());
            return;
        }

        // Mark token as used and user as verified
        vToken.setUsed(true);
        verificationTokenRepository.save(vToken);

        User user = vToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        response.sendRedirect(frontendUrl + "/email-verification.html?status=success");
    }

    // ── POST /api/auth/resend-verification ───────────────────────────────
    // Body: { "email": "user@gmail.com" }
    // Lets users request a fresh verification link if theirs expired.
    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, String>> resendVerification(
            @RequestBody Map<String, String> body) {

        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Email is required."));
        }

        User user = userRepository.findByEmail(email.trim().toLowerCase()).orElse(null);

        // Always respond with the same message to prevent email enumeration
        if (user == null || user.isEmailVerified()) {
            return ResponseEntity.ok(Map.of("message",
                    "If this email is registered and unverified, a new link has been sent."));
        }

        // Invalidate old tokens
        verificationTokenRepository.findAll().stream()
                .filter(t -> t.getUser().getId().equals(user.getId()) && !t.isUsed())
                .forEach(t -> {
                    t.setUsed(true);
                    verificationTokenRepository.save(t);
                });

        // Create fresh token (24 hours)
        String token = UUID.randomUUID().toString();
        EmailVerificationToken newToken = EmailVerificationToken.builder()
                .token(token)
                .user(user)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .used(false)
                .build();
        verificationTokenRepository.save(newToken);

        emailService.sendVerificationEmail(user.getEmail(), user.getName(), token);

        return ResponseEntity.ok(Map.of("message",
                "If this email is registered and unverified, a new link has been sent."));
    }
}