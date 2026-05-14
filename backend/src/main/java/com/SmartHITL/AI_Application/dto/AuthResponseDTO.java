package com.SmartHITL.AI_Application.dto;

public record AuthResponseDTO(
        String token,
        String role,
        String message
) {}
