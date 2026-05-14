package com.SmartHITL.AI_Application.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiClassificationResult {

    private String category;
    private int confidence;

}