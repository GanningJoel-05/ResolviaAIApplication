package com.SmartHITL.AI_Application.dto;

import jakarta.validation.constraints.*;

public record LoginRequestDTO(
        @Email
        @NotBlank
        String email,

        @NotBlank
        String password
) {}
