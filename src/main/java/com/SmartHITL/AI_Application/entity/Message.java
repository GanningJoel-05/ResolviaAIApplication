package com.SmartHITL.AI_Application.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Stores messages sent by blocked/deleted users to the admin who blocked them.
 * Also stores admin replies.
 */
@Entity
@Table(name = "block_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // User who sent this message
    @Column(name = "from_user_id", nullable = false)
    private Long fromUserId;

    // The admin this message is addressed to
    @Column(name = "to_admin_id", nullable = false)
    private Long toAdminId;

    // User's message text
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    // Admin's reply (null until admin replies)
    @Column(name = "admin_reply", columnDefinition = "TEXT")
    private String adminReply;

    // Sender's name (cached for display)
    @Column(name = "from_user_name")
    private String fromUserName;

    // Has the admin read this message yet?
    @Column(name = "is_read")
    @Builder.Default
    private boolean read = false;

    // Has the admin replied?
    @Column(name = "is_replied")
    @Builder.Default
    private boolean replied = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "replied_at")
    private LocalDateTime repliedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}