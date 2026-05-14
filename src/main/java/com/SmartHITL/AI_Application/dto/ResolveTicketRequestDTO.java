package com.SmartHITL.AI_Application.dto;

import lombok.Data;

/**
 * DTO received from the admin frontend when resolving a ticket.
 * <p>
 * Sent by: admin-ticket-detail.html → POST /api/admin/tickets/{id}/resolve
 * <p>
 * aiResolutionType values:
 *   "AUTO"     — Case 1: AI Solved  (confidence >= 70, no manual edit)
 *   "AI_HUMAN" — Case 2: AI + Human (admin edited the AI solution)
 *   "MANUAL"   — Case 3: Human Solved (admin wrote the solution manually)
 */
@Data
public class ResolveTicketRequestDTO {

    private String solution;

    private String status;

    private String aiResolutionType;

}