package com.SmartHITL.AI_Application.controller;

import com.SmartHITL.AI_Application.entity.Message;
import com.SmartHITL.AI_Application.entity.User;
import com.SmartHITL.AI_Application.repository.MessageRepository;
import com.SmartHITL.AI_Application.repository.UserRepository;
import com.SmartHITL.AI_Application.service.JwtService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageRepository messageRepository;
    private final UserRepository    userRepository;
    private final JwtService        jwtService;

    // ── Helper: get calling user from JWT ─────────────────────────────────
    private User getCallerUser(HttpServletRequest req) {
        String auth = req.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) return null;
        String email = jwtService.extractEmail(auth.substring(7));
        return userRepository.findByEmail(email).orElse(null);
    }

    // ══════════════════════════════════════════════════════════════════════
    // USER: Send message to admin who blocked them
    // POST /api/messages/send
    // Body: { "toAdminId": 1, "content": "Please unblock me..." }
    // ══════════════════════════════════════════════════════════════════════
    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(
            @RequestBody Map<String, Object> body,
            HttpServletRequest req) {

        User caller = getCallerUser(req);
        if (caller == null)
            return ResponseEntity.status(401).body("Unauthorized");

        Long adminId = Long.parseLong(body.get("toAdminId").toString());
        String content = (String) body.get("content");
        if (content == null || content.isBlank())
            return ResponseEntity.badRequest().body("Message content is required");

        // Verify the target admin exists
        userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        Message msg = Message.builder()
                .fromUserId(caller.getId())
                .fromUserName(caller.getName())
                .toAdminId(adminId)
                .content(content.trim())
                .read(false)
                .replied(false)
                .build();

        return ResponseEntity.ok(messageRepository.save(msg));
    }

    // ══════════════════════════════════════════════════════════════════════
    // USER: Get their own sent messages + admin replies
    // GET /api/messages/my
    // ══════════════════════════════════════════════════════════════════════
    @GetMapping("/my")
    public ResponseEntity<List<Message>> getMyMessages(HttpServletRequest req) {
        User caller = getCallerUser(req);
        if (caller == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(
                messageRepository.findByFromUserIdOrderByCreatedAtDesc(caller.getId())
        );
    }

    // ══════════════════════════════════════════════════════════════════════
    // ADMIN: Get all messages addressed to this admin
    // GET /api/messages/admin/inbox
    // ══════════════════════════════════════════════════════════════════════
    @GetMapping("/admin/inbox")
    public ResponseEntity<List<Message>> getAdminInbox(HttpServletRequest req) {
        User caller = getCallerUser(req);
        if (caller == null) return ResponseEntity.status(401).build();
        if (!"ADMIN".equals(caller.getRole().name()))
            return ResponseEntity.status(403).build();

        List<Message> messages = messageRepository
                .findByToAdminIdOrderByCreatedAtDesc(caller.getId());

        // Mark all as read when admin views inbox
        messages.forEach(m -> m.setRead(true));
        messageRepository.saveAll(messages);

        return ResponseEntity.ok(messages);
    }

    // ══════════════════════════════════════════════════════════════════════
    // ADMIN: Get unread count (for bell badge)
    // GET /api/messages/admin/unread-count
    // ══════════════════════════════════════════════════════════════════════
    @GetMapping("/admin/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(HttpServletRequest req) {
        User caller = getCallerUser(req);
        if (caller == null) return ResponseEntity.status(401).build();
        if (!"ADMIN".equals(caller.getRole().name()))
            return ResponseEntity.status(403).build();

        long count = messageRepository.countByToAdminIdAndReadFalse(caller.getId());
        return ResponseEntity.ok(Map.of("count", count));
    }

    // ══════════════════════════════════════════════════════════════════════
    // ADMIN: Reply to a message
    // POST /api/messages/{id}/reply
    // Body: { "reply": "Your account will be reviewed..." }
    // ══════════════════════════════════════════════════════════════════════
    @PostMapping("/{id}/reply")
    public ResponseEntity<?> replyToMessage(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            HttpServletRequest req) {

        User caller = getCallerUser(req);
        if (caller == null) return ResponseEntity.status(401).body("Unauthorized");
        if (!"ADMIN".equals(caller.getRole().name()))
            return ResponseEntity.status(403).body("Admin only");

        Message msg = messageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        // Ensure this admin is the intended recipient
        if (!msg.getToAdminId().equals(caller.getId()))
            return ResponseEntity.status(403).body("This message is not addressed to you");

        String replyText = body.get("reply");
        if (replyText == null || replyText.isBlank())
            return ResponseEntity.badRequest().body("Reply content is required");

        msg.setAdminReply(replyText.trim());
        msg.setReplied(true);
        msg.setRepliedAt(LocalDateTime.now());
        msg.setRead(true);

        return ResponseEntity.ok(messageRepository.save(msg));
    }
}