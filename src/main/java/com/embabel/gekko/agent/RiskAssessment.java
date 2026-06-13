package com.embabel.gekko.agent;

/**
 * Risk assessment result from the risk debate sub-process.
 * Contains the overall risk level and the reasoning behind it.
 */
public record RiskAssessment(RiskLevel level, String reasoning) {
    public RiskAssessment {
        if (level == null) {
            throw new IllegalArgumentException("Risk level must not be null");
        }
        if (reasoning == null || reasoning.isBlank()) {
            throw new IllegalArgumentException("Risk reasoning must not be blank");
        }
    }
}
