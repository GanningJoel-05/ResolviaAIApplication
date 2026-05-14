package com.SmartHITL.AI_Application.controller;

import com.SmartHITL.AI_Application.entity.Role;
import com.SmartHITL.AI_Application.entity.User;
import com.SmartHITL.AI_Application.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    // ── GET own profile ───────────────────────────────────────────────────
    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile(Principal principal) {
        if (principal == null)
            return ResponseEntity.status(401).body("Unauthorized");

        User user = userRepository
                .findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> response = new HashMap<>();
        response.put("id",            user.getId());
        response.put("name",          user.getName());
        response.put("username",      user.getUsername());
        response.put("email",         user.getEmail());
        response.put("role",          user.getRole() != null ? user.getRole().name() : null);
        response.put("createdAt",     user.getCreatedAt());
        response.put("accountStatus", user.getAccountStatus() != null ? user.getAccountStatus() : "ACTIVE");
        response.put("blockReason",   user.getBlockReason());
        response.put("blockedAt",     user.getBlockedAt());
        response.put("blockExpiresAt",user.getBlockExpiresAt());
        response.put("blockedByAdminId", user.getBlockedByAdminId());

        if (user.getBlockedByAdminId() != null) {
            String adminName = userRepository.findById(user.getBlockedByAdminId())
                    .map(User::getName)
                    .orElse("Administrator");
            response.put("blockedByAdminName", adminName);
        } else {
            response.put("blockedByAdminName", null);
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
                response.put("accountStatus", "ACTIVE");
                response.put("blockReason", null);
                response.put("blockedByAdminName", null);
            }
        }

        return ResponseEntity.ok(response);
    }

    // ── PUBLIC: Check if a username is available (used on register page) ─
    // GET /api/auth/check-username?username=joel_2005
    // Returns { "available": true/false }
    // Note: mapped under /api/auth/ so it's public (no JWT needed)

    // ── PATCH own name ────────────────────────────────────────────────────
    @PatchMapping("/profile/name")
    public ResponseEntity<Map<String, Object>> updateName(
            @RequestBody Map<String, String> body,
            Principal principal) {
        if (principal == null)
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String newName = body.get("name");
        if (newName == null || newName.isBlank())
            return ResponseEntity.badRequest().body(Map.of("message", "Name cannot be blank."));

        String cleanName = newName.replaceAll("[^\\p{L}\\p{N}\\p{Z}\\p{P}]", "").trim();
        if (cleanName.isBlank())
            return ResponseEntity.badRequest().body(Map.of("message", "Name contains only invalid characters. Emojis are not allowed."));

        if (user.getNameLastChanged() != null) {
            long daysSince = java.time.Duration.between(
                    user.getNameLastChanged(), java.time.LocalDateTime.now()).toDays();
            if (daysSince < 14) {
                long daysLeft = 14 - daysSince;
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Name change locked. You can change your name again in " +
                                daysLeft + " day(s). Limit: once every 14 days.",
                        "daysLeft", daysLeft,
                        "locked", true
                ));
            }
        }
        user.setName(cleanName);
        user.setNameLastChanged(java.time.LocalDateTime.now());
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Name updated successfully", "name", user.getName()));
    }

    // ── PATCH own username ────────────────────────────────────────────────
    @PatchMapping("/profile/username")
    public ResponseEntity<Map<String, Object>> updateUsername(
            @RequestBody Map<String, String> body,
            Principal principal) {
        if (principal == null)
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String newUsername = body.get("username");
        if (newUsername == null || newUsername.isBlank())
            return ResponseEntity.badRequest().body(Map.of("message", "Username cannot be blank."));

        newUsername = newUsername.toLowerCase().trim();
        if (!newUsername.matches("^[a-z0-9_]{3,20}$"))
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Username must be 3-20 characters. Only lowercase letters, numbers, and underscores are allowed."));

        if (userRepository.findByUsername(newUsername)
                .filter(u -> !u.getId().equals(user.getId())).isPresent())
            return ResponseEntity.badRequest().body(Map.of("message", "Username already taken. Please choose a different one."));

        int currentYear = java.time.LocalDate.now().getYear();
        int changeYear  = user.getUsernameLastChanged() != null
                ? user.getUsernameLastChanged().getYear() : 0;
        int count = (changeYear == currentYear)
                ? (user.getUsernameChangeCount() != null ? user.getUsernameChangeCount() : 0) : 0;

        if (count >= 2)
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Username change limit reached for " + currentYear +
                            ". You are allowed to change your username only 2 times per year.",
                    "locked", true
            ));

        user.setUsername(newUsername);
        user.setUsernameChangeCount(count + 1);
        user.setUsernameLastChanged(java.time.LocalDateTime.now());
        userRepository.save(user);
        return ResponseEntity.ok(Map.of(
                "message", "Username updated successfully",
                "username", user.getUsername(),
                "changesRemaining", 2 - (count + 1)
        ));
    }

    // ── GET all admins ────────────────────────────────────────────────────
    @GetMapping("/admins")
    public ResponseEntity<List<User>> getAdmins() {
        List<User> admins = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ADMIN
                        && !"DELETED".equals(u.getAccountStatus()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(admins);
    }
}