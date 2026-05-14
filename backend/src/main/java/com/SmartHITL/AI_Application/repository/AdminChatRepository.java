package com.SmartHITL.AI_Application.repository;

import com.SmartHITL.AI_Application.entity.AdminChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AdminChatRepository extends JpaRepository<AdminChatMessage, Long> {

    // All messages in a conversation between two admins (both directions), ordered by time
    @Query("SELECT m FROM AdminChatMessage m WHERE " +
            "(m.senderId = :a AND m.receiverId = :b) OR " +
            "(m.senderId = :b AND m.receiverId = :a) " +
            "ORDER BY m.createdAt ASC")
    List<AdminChatMessage> findConversation(@Param("a") Long adminA, @Param("b") Long adminB);

    // Unread count for a receiver from a specific sender
    long countBySenderIdAndReceiverIdAndReadFalse(Long senderId, Long receiverId);

    // Total unread for a specific receiver (all senders)
    long countByReceiverIdAndReadFalse(Long receiverId);
}