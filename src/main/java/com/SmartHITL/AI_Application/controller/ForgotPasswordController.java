package com.SmartHITL.AI_Application.controller;

import com.SmartHITL.AI_Application.dto.ForgotPasswordRequestDTO;
import com.SmartHITL.AI_Application.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class ForgotPasswordController {

    private final PasswordResetService passwordResetService;

    // ── POST /api/auth/forgot-password ────────────────────────────────────
    // Checks email exists, generates token, sends reset email.
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequestDTO request) {
        try {
            passwordResetService.initiatePasswordReset(request.getEmail());
            return ResponseEntity.ok(Map.of("message",
                    "If this email is registered, a password reset link has been sent."));
        } catch (RuntimeException e) {
            if ("EMAIL_NOT_FOUND".equals(e.getMessage())) {
                return ResponseEntity.status(404).body(Map.of("message",
                        "No account found with this email address."));
            }
            if ("EMAIL_NOT_VERIFIED".equals(e.getMessage())) {
                return ResponseEntity.status(403).body(Map.of("message",
                        "EMAIL_NOT_VERIFIED: Your email address is not verified yet. " +
                                "Please verify your email before resetting your password."));
            }
            return ResponseEntity.status(500).body(Map.of("message",
                    "Failed to send reset email. Please try again."));
        }
    }

    // ── GET /api/auth/validate-reset-token?token=... ──────────────────────
    // Called on page load by reset-password.html to check if token is still valid
    // before showing the form. Does NOT consume the token.
    @GetMapping("/validate-reset-token")
    public ResponseEntity<?> validateResetToken(@RequestParam String token) {
        try {
            passwordResetService.validateResetToken(token);
            return ResponseEntity.ok(Map.of("valid", true));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("valid", false, "message", e.getMessage()));
        }
    }

    // ── POST /api/auth/reset-password ─────────────────────────────────────
    // Now validates the token from the email link before resetting.
    // Body: { "token": "uuid-from-email", "newPassword": "..." }
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        String token       = body.get("token");
        String newPassword = body.get("newPassword");

        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message",
                    "Reset token is missing. Please use the link from your email."));
        }
        if (newPassword == null || newPassword.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message",
                    "New password is required."));
        }

        try {
            passwordResetService.resetPasswordWithToken(token, newPassword);
            return ResponseEntity.ok(Map.of("message", "Password reset successful"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}