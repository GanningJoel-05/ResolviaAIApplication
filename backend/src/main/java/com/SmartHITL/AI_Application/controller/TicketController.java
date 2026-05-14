package com.SmartHITL.AI_Application.controller;

import com.SmartHITL.AI_Application.entity.Ticket;
import com.SmartHITL.AI_Application.service.TicketService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    public Ticket createTicket(@RequestBody Ticket request) {
        return ticketService.createTicket(request);
    }

    @GetMapping("/my")
    public List<Ticket> getMyTickets() {
        return ticketService.getUserTickets();
    }

    @GetMapping("/{id}")
    public Ticket getTicket(@PathVariable Long id) {
        return ticketService.getTicket(id);
    }

    @PatchMapping("/{id}/rate")
    public ResponseEntity<Ticket> rateTicket(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body
    ) {
        int rating = (Integer) body.get("rating");
        String comment = body.containsKey("comment")
                ? (String) body.get("comment")
                : null;
        return ResponseEntity.ok(ticketService.rateTicket(id, rating, comment));
    }
}