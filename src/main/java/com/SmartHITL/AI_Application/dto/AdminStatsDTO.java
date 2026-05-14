package com.SmartHITL.AI_Application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminStatsDTO {

    private long totalTickets;
    private long openTickets;
    private long resolvedTickets;
    private long aiResolved;
    private long thisMonth;
    private double avgResolutionMinutesAI;
    private double avgResolutionMinutesHuman;

    public AdminStatsDTO(long totalTickets, long openTickets, long resolvedTickets,
                         long aiResolved, long thisMonth) {
        this.totalTickets    = totalTickets;
        this.openTickets     = openTickets;
        this.resolvedTickets = resolvedTickets;
        this.aiResolved      = aiResolved;
        this.thisMonth       = thisMonth;
        this.avgResolutionMinutesAI    = -1;
        this.avgResolutionMinutesHuman = -1;
    }
}