package com.SmartHITL.AI_Application.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AiClassifierService {

    private final GeminiServiceWrapper geminiServiceWrapper;

    public AiClassificationResult classify(String title, String description) {

        String prompt = """
                You are an IT support ticket classifier for a college/organization helpdesk.

                Classify this ticket and respond ONLY with valid JSON.
                No explanation, no markdown, no code blocks, no extra text.

                Title: %s
                Description: %s

                Rules for confidence scoring:

                  90-100 = Crystal-clear, single-step fix that any IT person knows
                           Examples: password reset, WiFi reconnect, restart app, clear cache,
                           forgotten PIN, printer offline toggle, Bluetooth on/off, screen brightness

                  75-89  = Common IT issue with a well-known standard solution
                           Examples: laptop overheating, software crash, app not opening,
                           email not syncing, slow computer, USB not detected, display issues,
                           network not connecting, virus scan needed, browser errors,
                           login failed, keyboard/mouse not working, audio issues

                  55-74  = Technical but environment-specific or multi-step diagnosis needed
                           Examples: server errors, VPN issues, database problems,
                           specific software configuration, multi-device network issues

                  20-54  = Vague, unclear, or requires physical inspection
                           Examples: "my computer is slow" with no details, intermittent issues,
                           hardware damage that needs hands-on assessment

                  0-19   = Non-technical, personal, or completely unrelated to IT
                           Examples: body pain, food, weather, homework help, personal issues

                IMPORTANT CALIBRATION:
                - A typical clear IT complaint (overheating, crash, not connecting) should score 75-85
                - Only drop below 55 if the issue is truly vague or needs physical inspection
                - Score 90+ only for single-step fixes (password reset, toggle WiFi, restart)
                - Score below 20 ONLY if clearly non-technical

                Category must be one of: Software, Hardware, Network, General

                Respond with ONLY this JSON, nothing else:
                {"category":"Software","confidence":78}
                """.formatted(title, description);

        String aiResponse = geminiServiceWrapper.generateContent(prompt);

        if (aiResponse == null || aiResponse.trim().startsWith("AI solution temporarily unavailable")) {
            return new AiClassificationResult("General", -1);
        }

        String category   = extractCategory(aiResponse);
        int    confidence = extractConfidence(aiResponse);

        return new AiClassificationResult(category, confidence);
    }

    private int extractConfidence(String response) {
        try {
            Pattern pattern = Pattern.compile(
                    "\"confidence\"\\s*:\\s*(\\d+)",
                    Pattern.CASE_INSENSITIVE
            );
            Matcher matcher = pattern.matcher(response);
            if (matcher.find()) {
                int value = Integer.parseInt(matcher.group(1));
                return Math.min(100, Math.max(0, value));
            }
        } catch (Exception e) {
            // Fall through to sentinel
        }
        return -1;
    }

    private String extractCategory(String response) {
        if (response == null) return "General";
        String lower = response.toLowerCase();
        if (lower.contains("hardware")) return "Hardware";
        if (lower.contains("software")) return "Software";
        if (lower.contains("network"))  return "Network";
        return "General";
    }

}