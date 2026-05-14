package com.SmartHITL.AI_Application.controller;

import com.SmartHITL.AI_Application.entity.AdminChatMessage;
import com.SmartHITL.AI_Application.entity.User;
import com.SmartHITL.AI_Application.repository.AdminChatRepository;
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
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class AdminChatController {

    private final AdminChatRepository chatRepository;
    private final UserRepository      userRepository;
    private final JwtService          jwtService;

    private User getCaller(HttpServletRequest req) {
        String auth = req.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) return null;
        String email = jwtService.extractEmail(auth.substring(7));
        return userRepository.findByEmail(email).orElse(null);
    }

    private boolean isAdmin(User u) {
        return u != null && u.getRole() != null && "ADMIN".equals(u.getRole().name());
    }

    // ── GET /api/chat/conversation/{otherAdminId} ──────────────────────────
    // Fetch all messages between calling admin and another admin, mark received as read
    @GetMapping("/conversation/{otherId}")
    public ResponseEntity<?> getConversation(@PathVariable Long otherId, HttpServletRequest req) {
        User me = getCaller(req);
        if (!isAdmin(me)) return ResponseEntity.status(403).build();

        List<AdminChatMessage> msgs = chatRepository.findConversation(me.getId(), otherId);

        // Mark messages addressed to me as read
        msgs.stream()
                .filter(m -> m.getReceiverId().equals(me.getId()) && !m.isRead())
                .forEach(m -> m.setRead(true));
        chatRepository.saveAll(msgs);

        return ResponseEntity.ok(msgs);
    }

    // ── POST /api/chat/send ────────────────────────────────────────────────
    @PostMapping("/send")
    public ResponseEntity<?> send(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        User me = getCaller(req);
        if (!isAdmin(me)) return ResponseEntity.status(403).body("Admin only");

        Long toId   = Long.parseLong(body.get("toAdminId").toString());
        String text = (String) body.get("content");
        if (text == null || text.isBlank())
            return ResponseEntity.badRequest().body("Content required");

        // Verify recipient is an admin
        User recipient = userRepository.findById(toId).orElse(null);
        if (!isAdmin(recipient))
            return ResponseEntity.badRequest().body("Recipient is not an admin");

        AdminChatMessage msg = AdminChatMessage.builder()
                .senderId(me.getId())
                .receiverId(toId)
                .senderName(me.getName())
                .content(text.trim())
                .build();

        return ResponseEntity.ok(chatRepository.save(msg));
    }

    // ── PATCH /api/chat/{id}/edit ──────────────────────────────────────────
    @PatchMapping("/{id}/edit")
    public ResponseEntity<?> editMessage(@PathVariable Long id,
                                         @RequestBody Map<String, String> body, HttpServletRequest req) {
        User me = getCaller(req);
        if (!isAdmin(me)) return ResponseEntity.status(403).build();

        AdminChatMessage msg = chatRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        if (!msg.getSenderId().equals(me.getId()))
            return ResponseEntity.status(403).body("You can only edit your own messages");
        if (msg.isDeleted())
            return ResponseEntity.badRequest().body("Cannot edit a deleted message");

        String newText = body.get("content");
        if (newText == null || newText.isBlank())
            return ResponseEntity.badRequest().body("Content required");

        msg.setEditedContent(newText.trim());
        msg.setEditedAt(LocalDateTime.now());
        return ResponseEntity.ok(chatRepository.save(msg));
    }

    // ── DELETE /api/chat/{id} ──────────────────────────────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMessage(@PathVariable Long id, HttpServletRequest req) {
        User me = getCaller(req);
        if (!isAdmin(me)) return ResponseEntity.status(403).build();

        AdminChatMessage msg = chatRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        if (!msg.getSenderId().equals(me.getId()))
            return ResponseEntity.status(403).body("You can only delete your own messages");

        msg.setDeleted(true);
        msg.setEditedAt(LocalDateTime.now());
        return ResponseEntity.ok(chatRepository.save(msg));
    }

    // ── GET /api/chat/unread-count ─────────────────────────────────────────
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount(HttpServletRequest req) {
        User me = getCaller(req);
        if (!isAdmin(me)) return ResponseEntity.status(403).build();
        long count = chatRepository.countByReceiverIdAndReadFalse(me.getId());
        return ResponseEntity.ok(Map.of("count", count));
    }
}