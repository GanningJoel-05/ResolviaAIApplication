package com.SmartHITL.AI_Application.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class GeminiServiceWrapper {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public String generateContent(String prompt){

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;

        Map<String, Object> body = Map.of(
                "contents", new Object[]{
                        Map.of("parts", new Object[]{
                                Map.of("text", prompt)
                        })
                },
                "generationConfig", Map.of(
                        "temperature", 0.1
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String,Object>> request =
                new HttpEntity<>(body,headers);

        try{

            ResponseEntity<Map> response =
                    restTemplate.postForEntity(url,request,Map.class);

            Map result = response.getBody();

            Map candidate =
                    (Map)((java.util.List)result.get("candidates")).get(0);

            Map content =
                    (Map)candidate.get("content");

            Map part =
                    (Map)((java.util.List)content.get("parts")).get(0);

            return part.get("text").toString();

        } catch (Exception e) {
            System.err.println("Gemini API error: " + e.getMessage());
            return "AI solution temporarily unavailable.";
        }

    }

}