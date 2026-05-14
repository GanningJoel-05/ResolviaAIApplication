package com.SmartHITL.AI_Application.service;

import com.SmartHITL.AI_Application.ai.AiClassificationResult;
import com.SmartHITL.AI_Application.ai.AiClassifierService;
import com.SmartHITL.AI_Application.ai.AiRoutingService;
import com.SmartHITL.AI_Application.ai.AiSolutionService;
import com.SmartHITL.AI_Application.entity.AdminActivityLog;
import com.SmartHITL.AI_Application.entity.Ticket;
import com.SmartHITL.AI_Application.entity.User;
import com.SmartHITL.AI_Application.repository.AdminActivityLogRepository;
import com.SmartHITL.AI_Application.repository.TicketRepository;
import com.SmartHITL.AI_Application.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository             ticketRepository;
    private final UserRepository               userRepository;
    private final AiClassifierService          aiClassifierService;
    private final AiRoutingService             aiRoutingService;
    private final AiSolutionService            aiSolutionService;
    private final EmailService                 emailService;
    private final AdminActivityLogRepository   activityLogRepository;

    private static final int AI_UNAVAILABLE = -1;

    public Ticket createTicket(Ticket ticketRequest) {

        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        long dupeCount = ticketRepository.countRecentDuplicates(
                user,
                ticketRequest.getTitle(),
                LocalDateTime.now().minusMinutes(5)
        );
        if (dupeCount > 0) {
            throw new RuntimeException(
                    "A similar ticket was already submitted in the last 5 minutes. " +
                            "Please wait before resubmitting."
            );
        }

        Ticket ticket = new Ticket();
        ticket.setTitle(ticketRequest.getTitle());
        ticket.setDescription(ticketRequest.getDescription());
        ticket.setCategory(ticketRequest.getCategory());
        ticket.setStatus("OPEN");
        ticket.setUser(user);
        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setTicketNumber(
                "SR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()
        );

        ticket = ticketRepository.save(ticket);

        String aiResponse = aiSolutionService.generateSolution(
                ticket.getTitle() + "\n" + ticket.getDescription()
        );

        boolean apiUnavailable = aiResponse != null &&
                aiResponse.trim().startsWith("AI solution temporarily unavailable");

        boolean isNonTechnical = !apiUnavailable &&
                aiResponse != null &&
                aiResponse.trim().toUpperCase().startsWith(
                        AiSolutionService.NON_TECHNICAL_MARKER
                );

        AiClassificationResult classification = apiUnavailable
                ? new AiClassificationResult("General", AI_UNAVAILABLE)
                : aiClassifierService.classify(ticket.getTitle(), ticket.getDescription());

        int    confidence = classification.getConfidence();
        String aiCategory = classification.getCategory();

        if (confidence != AI_UNAVAILABLE) {
            ticket.setCategory(aiCategory);
        }

        if (isNonTechnical) {
            ticket.setSolution(
                    "Your ticket has been reviewed by AI and identified as a non-technical issue. " +
                            "This support system handles IT and technical problems only.\n\n" +
                            "Examples of valid issues: hardware failures, software errors, network problems, " +
                            "login issues, printer/scanner issues, system crashes.\n\n" +
                            "Please resubmit with a genuine IT issue. Repeated non-technical submissions " +
                            "will result in a temporary block on ticket creation."
            );
            ticket.setAiConfidence(0);
            ticket.setPriority("LOW");
            ticket.setAiResolutionType("NON_TECHNICAL");
            ticket.setStatus("RESOLVED");

        } else if (confidence == AI_UNAVAILABLE) {
            ticket.setAiConfidence(null);
            ticket.setPriority("MEDIUM");
            ticket.setAiResolutionType("MANUAL");
            ticket.setSolution("AI service is currently unavailable. Admin will review manually.");
            ticket.setStatus("IN_PROGRESS");

        } else {
            ticket.setAiConfidence(confidence);

            if      (confidence >= 75) ticket.setPriority("HIGH");
            else if (confidence >= 50) ticket.setPriority("MEDIUM");
            else                       ticket.setPriority("LOW");

            String resolutionType = aiRoutingService.route(confidence);
            ticket.setAiResolutionType(resolutionType);

            if ("AUTO".equals(resolutionType)) {
                ticket.setSolution(aiResponse);
                ticket.setStatus("RESOLVED");
                ticket.setResolvedAt(LocalDateTime.now());
            } else {
                ticket.setSolution(aiResponse);
                ticket.setStatus("IN_PROGRESS");
            }
        }

        ticket = ticketRepository.save(ticket);

        // ── Feature 1: Send resolution email when AI auto-resolves ────────
        if ("RESOLVED".equals(ticket.getStatus())
                && "AUTO".equals(ticket.getAiResolutionType())) {
            try {
                emailService.sendTicketResolvedEmail(
                        user.getEmail(),
                        user.getName(),
                        ticket.getTicketNumber(),
                        ticket.getTitle(),
                        "AI System",
                        ticket.getSolution()
                );
            } catch (Exception e) {
                // Email failure must never break ticket creation
                System.err.println("Resolution email failed (non-fatal): " + e.getMessage());
            }
        }

        // ── Feature 3: Log AI auto-resolution in activity log ─────────────
        if ("AUTO".equals(ticket.getAiResolutionType())) {
            try {
                AdminActivityLog log = AdminActivityLog.builder()
                        .adminId(0L)
                        .adminName("AI System")
                        .actionType("TICKET_RESOLVED")
                        .description("AI auto-resolved ticket " + ticket.getTicketNumber()
                                + " — \"" + ticket.getTitle() + "\""
                                + " (Confidence: " + ticket.getAiConfidence() + "%)")
                        .targetTicketId(ticket.getId())
                        .targetTicketNumber(ticket.getTicketNumber())
                        .targetUserId(user.getId())
                        .targetUserName(user.getName())
                        .build();
                activityLogRepository.save(log);
            } catch (Exception e) {
                System.err.println("Activity log failed (non-fatal): " + e.getMessage());
            }
        }

        return ticket;
    }

    public List<Ticket> getUserTickets() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ticketRepository.findByUserId(user.getId());
    }

    public Ticket getTicket(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found with id: " + id));
    }

    public Ticket rateTicket(Long id, int rating, String comment) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found: " + id));

        if (!"RESOLVED".equals(ticket.getStatus())) {
            throw new RuntimeException("Can only rate resolved tickets");
        }
        if (rating < 1 || rating > 5) {
            throw new RuntimeException("Rating must be between 1 and 5");
        }

        ticket.setSatisfactionRating(rating);
        ticket.setSatisfactionComment(comment);
        return ticketRepository.save(ticket);
    }
}