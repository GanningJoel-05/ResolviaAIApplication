package com.SmartHITL.AI_Application.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminTestController {

    @GetMapping("/dashboard")
    public String adminDashboard() {
        return "Admin Dashboard Access Granted";
    }
}
