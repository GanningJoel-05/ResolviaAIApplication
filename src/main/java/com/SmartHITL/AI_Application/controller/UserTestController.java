package com.SmartHITL.AI_Application.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserTestController {

    @GetMapping("/dashboard")
    public String userDashboard() {
        return "User Dashboard Access Granted";
    }
}
