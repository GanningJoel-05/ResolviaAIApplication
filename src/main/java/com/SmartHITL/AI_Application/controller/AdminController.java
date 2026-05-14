package com.SmartHITL.AI_Application.controller;

import com.SmartHITL.AI_Application.dto.AdminStatsDTO;
import com.SmartHITL.AI_Application.dto.ResolveTicketRequestDTO;
import com.SmartHITL.AI_Application.dto.UserProfileDTO;
import com.SmartHITL.AI_Application.entity.Ticket;
import com.SmartHITL.AI_Application.entity.User;
import com.SmartHITL.AI_Application.repository.UserRepository;
import com.SmartHITL.AI_Application.service.AdminService;
import com.SmartHITL.AI_Application.service.JwtService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService   adminService;
    private final JwtService     jwtService;
    private final UserRepository userRepository;

    @GetMapping("/tickets")
    public List<Ticket> getAllTickets() { return adminService.getAllTickets(); }

    @GetMapping("/stats")
    public AdminStatsDTO getStats() { return adminService.getStats(); }

    @PostMapping("/tickets/{id}/resolve")
    public ResponseEntity<Ticket> resolveTicket(@PathVariable Long id,
                                                @RequestBody ResolveTicketRequestDTO request,
                                                HttpServletRequest req) {
        try {
            Long adminId   = getAdminId(req);
            User adminUser = userRepository.findById(adminId)
                    .orElseThrow(() -> new RuntimeException("Admin not found"));
            return ResponseEntity.ok(adminService.resolveTicketByAdmin(
                    id, request, adminId, adminUser.getName()));
        } catch (Exception e) {
            // fallback if admin extraction fails
            return ResponseEntity.ok(adminService.resolveTicket(id, request));
        }
    }

    // BATCH 5C: Ticket lock — acquire, refresh, release, check
    @PostMapping("/tickets/{id}/lock")
    public ResponseEntity<?> acquireLock(@PathVariable Long id, HttpServletRequest req) {
        Long adminId = getAdminId(req);
        User admin   = userRepository.findById(adminId).orElseThrow();
        return ResponseEntity.ok(adminService.acquireTicketLock(id, adminId, admin.getName()));
    }

    @PostMapping("/tickets/{id}/lock/refresh")
    public ResponseEntity<?> refreshLock(@PathVariable Long id, HttpServletRequest req) {
        adminService.refreshTicketLock(id, getAdminId(req));
        return ResponseEntity.ok(java.util.Map.of("ok", true));
    }

    @DeleteMapping("/tickets/{id}/lock")
    public ResponseEntity<?> releaseLock(@PathVariable Long id, HttpServletRequest req) {
        adminService.releaseTicketLock(id, getAdminId(req));
        return ResponseEntity.ok(java.util.Map.of("ok", true));
    }

    @GetMapping("/tickets/{id}/lock")
    public ResponseEntity<?> getLock(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getTicketLock(id));
    }

    // BATCH 5B: Admin views another admin's profile (ADMIN only)
    @GetMapping("/admin-profile/{id}")
    public ResponseEntity<?> getAdminProfile(@PathVariable Long id, HttpServletRequest req) {
        User target = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Admin not found"));
        if (target.getRole() == null || !"ADMIN".equals(target.getRole().name()))
            return ResponseEntity.badRequest().body("User is not an admin");

        long ticketsSolved = adminService.countTicketsSolvedByAdmin(id);
        long usersBlocked  = adminService.countUsersBlockedByAdmin(id);
        boolean isOnline   = adminService.isAdminOnline(id);

        java.util.Map<String, Object> profile = new java.util.HashMap<>();
        profile.put("id",           target.getId());
        profile.put("name",         target.getName());
        profile.put("username",     target.getUsername());
        profile.put("email",        target.getEmail());
        profile.put("createdAt",    target.getCreatedAt());
        profile.put("ticketsSolved",ticketsSolved);
        profile.put("usersBlocked", usersBlocked);
        profile.put("isOnline",     isOnline);
        return ResponseEntity.ok(profile);
    }

    // BATCH 5B: Get all admins stats summary (for admin-list / admin dashboard)
    @GetMapping("/admins/stats")
    public ResponseEntity<?> getAllAdminsStats() {
        return ResponseEntity.ok(adminService.getAllAdminsStats());
    }

    // BATCH 5A: Get all admins (for admin list page)
    @GetMapping("/admins")
    public ResponseEntity<List<User>> getAllAdmins() {
        return ResponseEntity.ok(adminService.getAllAdmins());
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserProfileDTO> getUserProfile(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getUserProfile(id));
    }

    @PatchMapping("/tickets/{id}/notes")
    public ResponseEntity<Ticket> saveAdminNotes(@PathVariable Long id,
                                                 @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(adminService.saveAdminNotes(id, body.getOrDefault("notes", "")));
    }

    @PatchMapping("/tickets/{id}/priority")
    public ResponseEntity<Ticket> overridePriority(@PathVariable Long id,
                                                   @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(adminService.overridePriority(id,
                body.getOrDefault("priority", "MEDIUM"), body.getOrDefault("reason", "")));
    }

    @GetMapping("/tickets/{id}/similar")
    public ResponseEntity<List<Ticket>> getSimilarTickets(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getSimilarTickets(id));
    }

    // ══════════════════════════════════════════════════════════════
    // USER BLOCK / DELETE SYSTEM
    // ══════════════════════════════════════════════════════════════

    /** POST /api/admin/users/{id}/block/temporary  body: {reason, hours} */
    @PostMapping("/users/{id}/block/temporary")
    public ResponseEntity<User> temporaryBlock(@PathVariable Long id,
                                               @RequestBody Map<String, Object> body, HttpServletRequest req) {
        Long adminId = getAdminId(req);
        String reason = (String) body.getOrDefault("reason", "");
        int hours = body.containsKey("hours")
                ? Integer.parseInt(body.get("hours").toString()) : 24;
        return ResponseEntity.ok(adminService.temporaryBlockUser(id, adminId, reason, hours));
    }

    /** POST /api/admin/users/{id}/block/permanent  body: {reason} */
    @PostMapping("/users/{id}/block/permanent")
    public ResponseEntity<User> permanentBlock(@PathVariable Long id,
                                               @RequestBody Map<String, String> body, HttpServletRequest req) {
        Long adminId = getAdminId(req);
        return ResponseEntity.ok(adminService.permanentBlockUser(id, adminId,
                body.getOrDefault("reason", "")));
    }

    /** POST /api/admin/users/{id}/unblock */
    @PostMapping("/users/{id}/unblock")
    public ResponseEntity<User> unblock(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.unblockUser(id));
    }

    /** POST /api/admin/users/{id}/delete  body: {reason}
     *  Using POST instead of DELETE to guarantee @RequestBody is received
     *  in all CORS/browser environments.
     */
    @PostMapping("/users/{id}/delete")
    public ResponseEntity<User> deleteUser(@PathVariable Long id,
                                           @RequestBody Map<String, String> body, HttpServletRequest req) {
        Long adminId = getAdminId(req);
        return ResponseEntity.ok(adminService.deleteUserAccount(id, adminId,
                body.getOrDefault("reason", "")));
    }

    /** GET /api/admin/blocked-users — returns users blocked by calling admin */
    @GetMapping("/blocked-users")
    public ResponseEntity<List<User>> getBlockedUsers(HttpServletRequest req) {
        return ResponseEntity.ok(adminService.getBlockedUsersByAdmin(getAdminId(req)));
    }

    // ── Feature 3: Activity Log endpoints ────────────────────────────────

    /** GET /api/admin/activity-log — all system-wide logs (newest first, max 200) */
    @GetMapping("/activity-log")
    public ResponseEntity<?> getActivityLog() {
        return ResponseEntity.ok(adminService.getAllActivityLogs());
    }

    /** GET /api/admin/activity-log/me — logs for the calling admin only */
    @GetMapping("/activity-log/me")
    public ResponseEntity<?> getMyActivityLog(HttpServletRequest req) {
        return ResponseEntity.ok(adminService.getActivityLogsByAdmin(getAdminId(req)));
    }

    // ── Extract admin ID from JWT in Authorization header ────────────────
    private Long getAdminId(HttpServletRequest req) {
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer "))
            throw new RuntimeException("Missing auth token");
        String email = jwtService.extractEmail(authHeader.substring(7));
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));
    }
}