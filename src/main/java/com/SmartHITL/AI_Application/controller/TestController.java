package com.SmartHITL.AI_Application.controller;

import com.SmartHITL.AI_Application.ai.GeminiServiceWrapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestController {

    private final GeminiServiceWrapper geminiServiceWrapper;

    public TestController(GeminiServiceWrapper geminiServiceWrapper){
        this.geminiServiceWrapper = geminiServiceWrapper;
    }

    @GetMapping
    public String tests() {
        return geminiServiceWrapper.generateContent("Laptop is Overheating - How can we solve this?");
    }

    @GetMapping("/ai-test")
    public String test(){
        return geminiServiceWrapper.generateContent("Give the response in 5 single Steps (step-1 to step-5)");
    }
}