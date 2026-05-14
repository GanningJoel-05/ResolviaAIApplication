package com.SmartHITL.AI_Application.repository;

import com.SmartHITL.AI_Application.entity.AdminActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminActivityLogRepository extends JpaRepository<AdminActivityLog, Long> {

    // All logs for a specific admin, newest first
    List<AdminActivityLog> findByAdminIdOrderByCreatedAtDesc(Long adminId);

    // All logs system-wide, newest first (for super admin view)
    List<AdminActivityLog> findAllByOrderByCreatedAtDesc();

    // Count actions by a specific admin
    long countByAdminId(Long adminId);
}