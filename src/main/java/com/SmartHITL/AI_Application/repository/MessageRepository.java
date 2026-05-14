package com.SmartHITL.AI_Application.repository;

import com.SmartHITL.AI_Application.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    // All messages sent TO a specific admin (for bell notifications)
    List<Message> findByToAdminIdOrderByCreatedAtDesc(Long adminId);

    // Unread messages for an admin (badge count)
    long countByToAdminIdAndReadFalse(Long adminId);

    // Messages FROM a specific user (so user can see their sent messages + replies)
    List<Message> findByFromUserIdOrderByCreatedAtDesc(Long userId);

    // Most recent message from a user to an admin (1 conversation per pair)
    List<Message> findByFromUserIdAndToAdminIdOrderByCreatedAtDesc(Long userId, Long adminId);
}