package com.SmartHITL.AI_Application.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name="tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ticketNumber;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String category;

    private String status;

    @Column(columnDefinition = "TEXT")
    private String solution;

    @Column(name="ai_confidence")
    private Integer aiConfidence;

    @Column(name="ai_resolution_type")
    private String aiResolutionType;

    // Priority: HIGH (>=75%), MEDIUM (50-74%), LOW (<50%)
    private String priority;

    private LocalDateTime createdAt;

    // ── BATCH 2: Resolution Time Tracking ──────────────────────────────────
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    // ── BATCH 5A: Track which admin resolved this ticket ─────────────────
    // null for AUTO-resolved tickets (AI resolved, no admin)
    @Column(name = "resolved_by_admin_id")
    private Long resolvedByAdminId;

    @Column(name = "resolved_by_admin_name")
    private String resolvedByAdminName;

    // ── BATCH 3: Admin Notes (internal — never shown to users) ───────────────
    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    // ── BATCH 3: Priority Override ────────────────────────────────────────
    // Admin can manually override AI-assigned priority with a reason
    @Column(name = "priority_override_reason")
    private String priorityOverrideReason;

    // ── BATCH 2: Satisfaction Rating ───────────────────────────────────────
    // User rates the solution after viewing it
    // 1-5 stars, null = not rated yet
    @Column(name = "satisfaction_rating")
    private Integer satisfactionRating;

    // Optional comment with the rating
    @Column(name = "satisfaction_comment", columnDefinition = "TEXT")
    private String satisfactionComment;

    @ManyToOne
    @JoinColumn(name="user_id")
    private User user;
}