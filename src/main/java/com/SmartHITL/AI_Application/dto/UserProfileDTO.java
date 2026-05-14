package com.SmartHITL.AI_Application.dto;

import com.SmartHITL.AI_Application.entity.Ticket;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO returned by GET /api/admin/users/{id}
 * Gives admin a full view of any user's profile + their tickets.
 * Does NOT expose the user's password.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileDTO {

    private Long   id;
    private String name;
    private String username;   // BATCH 5B: unique username
    private String email;
    private String role;
    private LocalDateTime createdAt;

    // Ticket summary counts
    private long totalTickets;
    private long resolvedTickets;
    private long pendingTickets;

    // Last 5 tickets for the mini-list
    private List<Ticket> recentTickets;

    // ── Block Status (visible to admin) ───────────────────────────────────
    // ACTIVE / TEMPORARY / PERMANENT / DELETED
    private String accountStatus;
    private String blockReason;
    private LocalDateTime blockedAt;
    private LocalDateTime blockExpiresAt;
    private Long   blockedByAdminId;    // admin who blocked this user
    private String blockedByAdminName;  // resolved from ID for display
}