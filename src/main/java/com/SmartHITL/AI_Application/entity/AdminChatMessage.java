package com.SmartHITL.AI_Application.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "admin_chat_messages")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AdminChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "sender_name")
    private String senderName;

    // null = not edited/deleted
    @Column(name = "edited_content", columnDefinition = "TEXT")
    private String editedContent;

    @Column(name = "is_deleted")
    @Builder.Default
    private boolean deleted = false;

    @Column(name = "is_read")
    @Builder.Default
    private boolean read = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "edited_at")
    private LocalDateTime editedAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public String getDisplayContent() {
        if (deleted)       return "🗑 This message was deleted";
        if (editedContent != null) return editedContent;
        return content;
    }
}