package com.SmartHITL.AI_Application.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiSolutionService {

    private final GeminiServiceWrapper geminiServiceWrapper;

    public static final String NON_TECHNICAL_MARKER = "NOT_TECHNICAL";

    public String generateSolution(String problem) {

        String prompt = """
                You are a strict IT technical support assistant for an organization.

                A user submitted the following support ticket:
                %s

                STEP 1 — Classify the request:
                Is this a genuine IT or technical support issue?

                Examples of VALID technical issues:
                  - Laptop, desktop, computer, mobile phone problems
                  - Overheating, not starting, screen issues, keyboard broken
                  - WiFi, internet, VPN, network not connecting
                  - Software crash, app error, system slow, blue screen
                  - Password reset, email not working, login failed
                  - Printer, scanner, projector not working
                  - Server down, website not loading, database error
                  - Storage full, virus, malware, system update issues

                Examples of INVALID non-technical requests (REJECT THESE):
                  - Body pain, medical issues (leg pain, headache, stomach)
                  - Personal items (broken pencil, lost bag, torn clothes)
                  - Food, weather, relationships, emotions
                  - Jokes, greetings, random unrelated text
                  - Anything not related to IT or technology

                STEP 2 — Respond:

                If NON-TECHNICAL → respond with ONLY this one word, nothing else:
                NOT_TECHNICAL

                If VALID TECHNICAL → respond in this exact plain text format ONLY.
                No markdown. No asterisks (*). No hashtags (#). No bold. No bullets.
                No sentence before Step 1. No sentence after Step 5:

                Step 1: [one clear concise action]
                Step 2: [one clear concise action]
                Step 3: [one clear concise action]
                Step 4: [one clear concise action]
                Step 5: [one clear concise action]
                """.formatted(problem);

        return geminiServiceWrapper.generateContent(prompt);
    }

}