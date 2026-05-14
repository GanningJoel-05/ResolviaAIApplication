package com.SmartHITL.AI_Application.service;

import com.SmartHITL.AI_Application.dto.AdminStatsDTO;
import com.SmartHITL.AI_Application.dto.ResolveTicketRequestDTO;
import com.SmartHITL.AI_Application.dto.UserProfileDTO;
import com.SmartHITL.AI_Application.entity.AdminActivityLog;
import com.SmartHITL.AI_Application.entity.Ticket;
import com.SmartHITL.AI_Application.entity.User;
import com.SmartHITL.AI_Application.repository.AdminActivityLogRepository;
import com.SmartHITL.AI_Application.repository.TicketRepository;
import com.SmartHITL.AI_Application.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final TicketRepository           ticketRepository;
    private final UserRepository             userRepository;
    private final EmailService               emailService;
    private final AdminActivityLogRepository activityLogRepository;

    // ── Helpers ───────────────────────────────────────────────────────────

    private void log(Long adminId, String adminName, String actionType,
                     String description, Ticket ticket, User targetUser) {
        try {
            AdminActivityLog entry = AdminActivityLog.builder()
                    .adminId(adminId)
                    .adminName(adminName)
                    .actionType(actionType)
                    .description(description)
                    .targetTicketId(ticket != null ? ticket.getId() : null)
                    .targetTicketNumber(ticket != null ? ticket.getTicketNumber() : null)
                    .targetUserId(targetUser != null ? targetUser.getId() : null)
                    .targetUserName(targetUser != null ? targetUser.getName() : null)
                    .build();
            activityLogRepository.save(entry);
        } catch (Exception e) {
            System.err.println("Activity log failed (non-fatal): " + e.getMessage());
        }
    }

    // ── Tickets ───────────────────────────────────────────────────────────

    public List<Ticket> getAllTickets() {
        return ticketRepository.findAll();
    }

    public AdminStatsDTO getStats() {
        long total      = ticketRepository.count();
        long open       = ticketRepository.countByStatus("OPEN")
                + ticketRepository.countByStatus("IN_PROGRESS");
        long resolved   = ticketRepository.countByStatus("RESOLVED");
        long aiResolved = ticketRepository.countByAiResolutionType("AUTO");
        long thisMonth  = ticketRepository.countByCreatedAtAfter(
                LocalDate.now().withDayOfMonth(1).atStartOfDay());

        List<Ticket> all = ticketRepository.findAll();

        double avgAI = all.stream()
                .filter(t -> "AUTO".equals(t.getAiResolutionType())
                        && t.getCreatedAt() != null && t.getResolvedAt() != null)
                .mapToLong(t -> Duration.between(t.getCreatedAt(), t.getResolvedAt()).toSeconds())
                .average().orElse(-1);

        double avgHuman = all.stream()
                .filter(t -> ("MANUAL".equals(t.getAiResolutionType())
                        || "AI_HUMAN".equals(t.getAiResolutionType()))
                        && t.getCreatedAt() != null && t.getResolvedAt() != null)
                .mapToLong(t -> Duration.between(t.getCreatedAt(), t.getResolvedAt()).toMinutes())
                .average().orElse(-1);

        return new AdminStatsDTO(total, open, resolved, aiResolved, thisMonth,
                avgAI > 0 ? avgAI / 60.0 : -1, avgHuman);
    }

    public Ticket resolveTicket(Long ticketId, ResolveTicketRequestDTO request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketId));
        if ("RESOLVED".equals(ticket.getStatus()))
            throw new RuntimeException("Ticket is already resolved");
        ticket.setStatus("RESOLVED");
        ticket.setSolution(request.getSolution());
        ticket.setAiResolutionType(request.getAiResolutionType());
        ticket.setResolvedAt(LocalDateTime.now());
        return ticketRepository.save(ticket);
    }

    // BATCH 5A + Feature 1 + Feature 3: resolve with admin tracking, email, log
    public Ticket resolveTicketByAdmin(Long ticketId, ResolveTicketRequestDTO request,
                                       Long adminId, String adminName) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketId));
        if ("RESOLVED".equals(ticket.getStatus()))
            throw new RuntimeException("Ticket is already resolved");

        ticket.setStatus("RESOLVED");
        ticket.setSolution(request.getSolution());
        ticket.setAiResolutionType(request.getAiResolutionType());
        ticket.setResolvedAt(LocalDateTime.now());

        if (!"AUTO".equals(request.getAiResolutionType())) {
            ticket.setResolvedByAdminId(adminId);
            ticket.setResolvedByAdminName(adminName);
        }

        ticket = ticketRepository.save(ticket);

        // ── Feature 1: Send resolution email to the user ──────────────────
        try {
            User ticketUser = ticket.getUser();
            if (ticketUser != null) {
                String resolvedByLabel = "MANUAL".equals(request.getAiResolutionType())
                        ? adminName
                        : "AI + " + adminName;
                emailService.sendTicketResolvedEmail(
                        ticketUser.getEmail(),
                        ticketUser.getName(),
                        ticket.getTicketNumber(),
                        ticket.getTitle(),
                        resolvedByLabel,
                        ticket.getSolution()
                );
            }
        } catch (Exception e) {
            System.err.println("Resolution email failed (non-fatal): " + e.getMessage());
        }

        // ── Feature 3: Log the admin resolve action ───────────────────────
        String typeLabel = "MANUAL".equals(request.getAiResolutionType())
                ? "Manual" : "AI + Human";
        log(adminId, adminName, "TICKET_RESOLVED",
                "Admin resolved ticket " + ticket.getTicketNumber()
                        + " — \"" + ticket.getTitle() + "\""
                        + " [" + typeLabel + "]",
                ticket, ticket.getUser());

        return ticket;
    }

    // ── Activity Log queries ──────────────────────────────────────────────

    /** All activity logs system-wide, newest first. Limit to last 200 entries. */
    public List<AdminActivityLog> getAllActivityLogs() {
        return activityLogRepository.findAllByOrderByCreatedAtDesc()
                .stream().limit(200).collect(Collectors.toList());
    }

    /** Activity logs for a specific admin. */
    public List<AdminActivityLog> getActivityLogsByAdmin(Long adminId) {
        return activityLogRepository.findByAdminIdOrderByCreatedAtDesc(adminId);
    }

    // ── Admin list / profile ──────────────────────────────────────────────

    public List<User> getAllAdmins() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() != null && "ADMIN".equals(u.getRole().name()))
                .collect(Collectors.toList());
    }

    public UserProfileDTO getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        List<Ticket> tickets = ticketRepository.findByUserId(userId);
        long total    = tickets.size();
        long resolved = tickets.stream().filter(t -> "RESOLVED".equals(t.getStatus())).count();
        long pending  = tickets.stream()
                .filter(t -> "OPEN".equals(t.getStatus()) || "IN_PROGRESS".equals(t.getStatus())).count();
        List<Ticket> recent = tickets.stream()
                .sorted(Comparator.comparing(Ticket::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5).collect(Collectors.toList());

        String blockedByName = null;
        if (user.getBlockedByAdminId() != null) {
            blockedByName = userRepository.findById(user.getBlockedByAdminId())
                    .map(User::getName).orElse("Admin #" + user.getBlockedByAdminId());
        }

        UserProfileDTO dto = new UserProfileDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole().name());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setTotalTickets(total);
        dto.setResolvedTickets(resolved);
        dto.setPendingTickets(pending);
        dto.setRecentTickets(recent);
        dto.setAccountStatus(user.getAccountStatus() != null ? user.getAccountStatus() : "ACTIVE");
        dto.setBlockReason(user.getBlockReason());
        dto.setBlockedAt(user.getBlockedAt());
        dto.setBlockExpiresAt(user.getBlockExpiresAt());
        dto.setBlockedByAdminId(user.getBlockedByAdminId());
        dto.setBlockedByAdminName(blockedByName);
        return dto;
    }

    // ── Admin Notes ───────────────────────────────────────────────────────

    public Ticket saveAdminNotes(Long ticketId, String notes) {
        Ticket t = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketId));
        t.setAdminNotes(notes);
        return ticketRepository.save(t);
    }

    // ── Priority Override ─────────────────────────────────────────────────

    public Ticket overridePriority(Long ticketId, String priority, String reason) {
        Ticket t = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketId));
        if (!priority.equals("HIGH") && !priority.equals("MEDIUM") && !priority.equals("LOW"))
            throw new RuntimeException("Invalid priority: " + priority);
        t.setPriority(priority);
        t.setPriorityOverrideReason(reason);
        return ticketRepository.save(t);
    }

    // ── Similar Tickets ───────────────────────────────────────────────────

    public List<Ticket> getSimilarTickets(Long ticketId) {
        Ticket current = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketId));
        return ticketRepository
                .findByCategoryAndStatusOrderByCreatedAtDesc(current.getCategory(), "RESOLVED")
                .stream().filter(t -> !t.getId().equals(ticketId))
                .limit(5).collect(Collectors.toList());
    }

    // ══════════════════════════════════════════════════════════════════════
    // USER BLOCK / DELETE SYSTEM  (Feature 3: all actions are logged)
    // ══════════════════════════════════════════════════════════════════════

    private User findNonAdminUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        if ("ADMIN".equals(user.getRole().name()))
            throw new RuntimeException("Cannot modify another admin account.");
        return user;
    }

    public User temporaryBlockUser(Long userId, Long adminId, String reason, int hours) {
        if (reason == null || reason.isBlank())
            throw new RuntimeException("A reason is required to block a user.");
        if (hours < 1)
            throw new RuntimeException("Block duration must be at least 1 hour.");
        User user = findNonAdminUser(userId);
        user.setAccountStatus("TEMPORARY");
        user.setBlockReason(reason.trim());
        user.setBlockedAt(LocalDateTime.now());
        user.setBlockExpiresAt(LocalDateTime.now().plusHours(hours));
        user.setBlockedByAdminId(adminId);
        User saved = userRepository.save(user);

        String adminName = userRepository.findById(adminId).map(User::getName).orElse("Admin");
        log(adminId, adminName, "USER_BLOCKED_TEMP",
                "Temporarily blocked user \"" + user.getName() + "\" for " + hours
                        + " hour(s). Reason: " + reason.trim(),
                null, user);
        return saved;
    }

    public User permanentBlockUser(Long userId, Long adminId, String reason) {
        if (reason == null || reason.isBlank())
            throw new RuntimeException("A reason is required to permanently block a user.");
        User user = findNonAdminUser(userId);
        user.setAccountStatus("PERMANENT");
        user.setBlockReason(reason.trim());
        user.setBlockedAt(LocalDateTime.now());
        user.setBlockExpiresAt(null);
        user.setBlockedByAdminId(adminId);
        User saved = userRepository.save(user);

        String adminName = userRepository.findById(adminId).map(User::getName).orElse("Admin");
        log(adminId, adminName, "USER_BLOCKED_PERM",
                "Permanently blocked user \"" + user.getName() + "\". Reason: " + reason.trim(),
                null, user);
        return saved;
    }

    public User unblockUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        Long adminId = user.getBlockedByAdminId();
        user.setAccountStatus("ACTIVE");
        user.setBlockReason(null);
        user.setBlockedAt(null);
        user.setBlockExpiresAt(null);
        user.setBlockedByAdminId(null);
        User saved = userRepository.save(user);

        if (adminId != null) {
            String adminName = userRepository.findById(adminId).map(User::getName).orElse("Admin");
            log(adminId, adminName, "USER_UNBLOCKED",
                    "Unblocked / restored user \"" + user.getName() + "\"",
                    null, user);
        }
        return saved;
    }

    public User deleteUserAccount(Long userId, Long adminId, String reason) {
        if (reason == null || reason.isBlank())
            throw new RuntimeException("A reason is required to delete a user account.");
        User user = findNonAdminUser(userId);
        user.setAccountStatus("DELETED");
        user.setBlockReason(reason.trim());
        user.setBlockedAt(LocalDateTime.now());
        user.setBlockExpiresAt(null);
        user.setBlockedByAdminId(adminId);
        User saved = userRepository.save(user);

        String adminName = userRepository.findById(adminId).map(User::getName).orElse("Admin");
        log(adminId, adminName, "USER_DELETED",
                "Deleted account of user \"" + user.getName() + "\". Reason: " + reason.trim(),
                null, user);
        return saved;
    }

    public List<User> getBlockedUsersByAdmin(Long adminId) {
        return userRepository.findAll().stream()
                .filter(u -> adminId.equals(u.getBlockedByAdminId())
                        && !"ACTIVE".equals(u.getAccountStatus()))
                .sorted(Comparator.comparing(User::getBlockedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    // ── BATCH 5B: Admin profile stats ────────────────────────────────────

    public long countTicketsSolvedByAdmin(Long adminId) {
        return ticketRepository.findAll().stream()
                .filter(t -> adminId.equals(t.getResolvedByAdminId()))
                .count();
    }

    public long countUsersBlockedByAdmin(Long adminId) {
        return userRepository.findAll().stream()
                .filter(u -> adminId.equals(u.getBlockedByAdminId()))
                .count();
    }

    public boolean isAdminOnline(Long adminId) {
        return false; // overridden by frontend localStorage lastSeen logic
    }

    public java.util.List<java.util.Map<String, Object>> getAllAdminsStats() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() != null && "ADMIN".equals(u.getRole().name())
                        && !"DELETED".equals(u.getAccountStatus()))
                .map(admin -> {
                    java.util.Map<String, Object> m = new java.util.HashMap<>();
                    m.put("id",           admin.getId());
                    m.put("name",         admin.getName());
                    m.put("username",     admin.getUsername());
                    m.put("email",        admin.getEmail());
                    m.put("createdAt",    admin.getCreatedAt());
                    m.put("ticketsSolved",countTicketsSolvedByAdmin(admin.getId()));
                    m.put("usersBlocked", countUsersBlockedByAdmin(admin.getId()));
                    return m;
                })
                .collect(Collectors.toList());
    }

    // ── BATCH 5C: Ticket lock ─────────────────────────────────────────────

    private final java.util.concurrent.ConcurrentHashMap<Long, Long>   ticketLocks      = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ConcurrentHashMap<Long, String> ticketLockNames  = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ConcurrentHashMap<Long, Long>   ticketLockTimestamps = new java.util.concurrent.ConcurrentHashMap<>();

    private static final long LOCK_TTL_MS = 5 * 60 * 1000;

    public java.util.Map<String, Object> acquireTicketLock(Long ticketId, Long adminId, String adminName) {
        Long lockHolder = ticketLocks.get(ticketId);
        long now = System.currentTimeMillis();
        Long ts = ticketLockTimestamps.get(ticketId);
        if (lockHolder != null && ts != null && (now - ts) > LOCK_TTL_MS) {
            ticketLocks.remove(ticketId);
            lockHolder = null;
        }
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        if (lockHolder == null || lockHolder.equals(adminId)) {
            ticketLocks.put(ticketId, adminId);
            ticketLockNames.put(ticketId, adminName);
            ticketLockTimestamps.put(ticketId, now);
            result.put("locked", true);
            result.put("lockedByMe", true);
            result.put("adminName", adminName);
        } else {
            result.put("locked", true);
            result.put("lockedByMe", false);
            result.put("adminName", ticketLockNames.getOrDefault(ticketId, "Another admin"));
        }
        return result;
    }

    public void refreshTicketLock(Long ticketId, Long adminId) {
        if (adminId.equals(ticketLocks.get(ticketId))) {
            ticketLockTimestamps.put(ticketId, System.currentTimeMillis());
        }
    }

    public void releaseTicketLock(Long ticketId, Long adminId) {
        if (adminId.equals(ticketLocks.get(ticketId))) {
            ticketLocks.remove(ticketId);
            ticketLockNames.remove(ticketId);
            ticketLockTimestamps.remove(ticketId);
        }
    }

    public java.util.Map<String, Object> getTicketLock(Long ticketId) {
        Long lockHolder = ticketLocks.get(ticketId);
        long now = System.currentTimeMillis();
        Long ts  = ticketLockTimestamps.get(ticketId);
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        if (lockHolder != null && ts != null && (now - ts) <= LOCK_TTL_MS) {
            result.put("locked", true);
            result.put("adminId", lockHolder);
            result.put("adminName", ticketLockNames.getOrDefault(ticketId, "An admin"));
        } else {
            if (lockHolder != null) {
                ticketLocks.remove(ticketId);
                ticketLockNames.remove(ticketId);
            }
            result.put("locked", false);
        }
        return result;
    }
}