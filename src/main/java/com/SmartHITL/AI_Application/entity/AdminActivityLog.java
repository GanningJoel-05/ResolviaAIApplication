package com.SmartHITL.AI_Application.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Stores a permanent audit trail of admin actions.
 * Every resolve, block, unblock, delete, and restore is recorded here.
 */
@Entity
@Table(name = "admin_activity_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Which admin performed the action
    @Column(name = "admin_id", nullable = false)
    private Long adminId;

    @Column(name = "admin_name", nullable = false)
    private String adminName;

    // What action was taken
    // Values: TICKET_RESOLVED, USER_BLOCKED_TEMP, USER_BLOCKED_PERM,
    //         USER_UNBLOCKED, USER_DELETED, USER_RESTORED
    @Column(name = "action_type", nullable = false)
    private String actionType;

    // Human-readable description of the action
    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    // Optional: which ticket or user was affected
    @Column(name = "target_ticket_id")
    private Long targetTicketId;

    @Column(name = "target_ticket_number")
    private String targetTicketNumber;

    @Column(name = "target_user_id")
    private Long targetUserId;

    @Column(name = "target_user_name")
    private String targetUserName;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}