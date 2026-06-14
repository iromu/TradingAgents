package com.embabel.gekko.agent;

/**
 * Structured LLM output for risk assessment — returned via createObject().
 */
public record RiskAssessmentOutput(
        RiskLevel riskLevel,
        String reasoning
) {
    public RiskAssessmentOutput {
        if (riskLevel == null) {
            throw new IllegalArgumentException("riskLevel must not be null");
        }
        if (reasoning == null || reasoning.isBlank()) {
            throw new IllegalArgumentException("reasoning must not be blank");
        }
    }
}
