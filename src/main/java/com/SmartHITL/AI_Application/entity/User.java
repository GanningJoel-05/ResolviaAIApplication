package com.SmartHITL.AI_Application.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true)
    private String email;

    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    private LocalDateTime createdAt;

    // ── Email Verification ─────────────────────────────────────────────────
    // false until the user clicks the verification link in their email
    @Column(name = "email_verified")
    @Builder.Default
    private boolean emailVerified = false;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // ── BATCH 5B: Username System ─────────────────────────────────────────
    @Column(name = "username", unique = true)
    private String username;

    @Column(name = "username_change_count")
    @Builder.Default
    private Integer usernameChangeCount = 0;

    @Column(name = "username_last_changed")
    private LocalDateTime usernameLastChanged;

    // ── BATCH 5B: Name Edit Limit (once per 14 days) ───────────────────────
    @Column(name = "name_last_changed")
    private LocalDateTime nameLastChanged;

    // ── Account Block / Delete System ──────────────────────────────────────
    @Column(name = "account_status")
    @Builder.Default
    private String accountStatus = "ACTIVE";

    @Column(name = "block_reason", columnDefinition = "TEXT")
    private String blockReason;

    @Column(name = "blocked_at")
    private LocalDateTime blockedAt;

    @Column(name = "block_expires_at")
    private LocalDateTime blockExpiresAt;

    @Column(name = "blocked_by_admin_id")
    private Long blockedByAdminId;
}