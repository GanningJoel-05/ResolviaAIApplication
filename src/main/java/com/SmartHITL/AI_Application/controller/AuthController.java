package com.SmartHITL.AI_Application.controller;

import com.SmartHITL.AI_Application.dto.AuthResponseDTO;
import com.SmartHITL.AI_Application.dto.LoginRequestDTO;
import com.SmartHITL.AI_Application.dto.RegisterRequestDTO;
import com.SmartHITL.AI_Application.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public AuthResponseDTO register(@Valid @RequestBody RegisterRequestDTO request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponseDTO login(@Valid @RequestBody LoginRequestDTO request) {
        return authService.login(request);
    }
}
