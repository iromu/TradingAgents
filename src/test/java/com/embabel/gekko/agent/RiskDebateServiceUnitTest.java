package com.embabel.gekko.agent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RiskDebateService parseRiskAssessment covering all 3 risk levels.
 */
class RiskDebateServiceUnitTest {

    private RiskDebateService createService() {
        return new RiskDebateService();
    }

    @Test
    void parseRiskAssessment_risky() {
        var service = createService();
        var result = invokeParseRiskAssessment(service, "buy because of high risk appetite");
        assertEquals(RiskLevel.RISKY, result.level());
        assertTrue(result.reasoning().toLowerCase().contains("buy"));
    }

    @Test
    void parseRiskAssessment_risky_bold() {
        var service = createService();
        var result = invokeParseRiskAssessment(service, "bold buy aggressive move");
        assertEquals(RiskLevel.RISKY, result.level());
    }

    @Test
    void parseRiskAssessment_conservative_sell() {
        var service = createService();
        var result = invokeParseRiskAssessment(service, "sell to avoid risk");
        assertEquals(RiskLevel.CONSERVATIVE, result.level());
    }

    @Test
    void parseRiskAssessment_conservative_cautious() {
        var service = createService();
        var result = invokeParseRiskAssessment(service, "cautious approach recommended");
        assertEquals(RiskLevel.CONSERVATIVE, result.level());
    }

    @Test
    void parseRiskAssessment_conservative_safe() {
        var service = createService();
        var result = invokeParseRiskAssessment(service, "safe investment strategy");
        assertEquals(RiskLevel.CONSERVATIVE, result.level());
    }

    @Test
    void parseRiskAssessment_neutral_default() {
        var service = createService();
        var result = invokeParseRiskAssessment(service, "hold position with moderate outlook");
        assertEquals(RiskLevel.NEUTRAL, result.level());
    }

    @Test
    void parseRiskAssessment_neutral_empty() {
        var service = createService();
        var result = invokeParseRiskAssessment(service, "");
        assertEquals(RiskLevel.NEUTRAL, result.level());
        assertTrue(result.reasoning().contains("empty"));
    }

    @Test
    void parseRiskAssessment_neutral_null() {
        var service = createService();
        var result = invokeParseRiskAssessment(service, null);
        assertEquals(RiskLevel.NEUTRAL, result.level());
        assertTrue(result.reasoning().contains("empty"));
    }

    @Test
    void parseRiskAssessment_reasoning_truncated() {
        var service = createService();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 250; i++) {
            sb.append("a");
        }
        var result = invokeParseRiskAssessment(service, sb.toString());
        assertEquals(RiskLevel.NEUTRAL, result.level());
        // Reasoning is truncated to 200 chars + "..." = 203 chars max
        assertTrue(result.reasoning().length() <= 203);
    }

    @Test
    void parseRiskAssessment_risky_high() {
        var service = createService();
        var result = invokeParseRiskAssessment(service, "buy high risk position");
        assertEquals(RiskLevel.RISKY, result.level());
    }

    private RiskAssessment invokeParseRiskAssessment(RiskDebateService service, String input) {
        try {
            var method = RiskDebateService.class.getDeclaredMethod("parseRiskAssessment", String.class);
            method.setAccessible(true);
            return (RiskAssessment) method.invoke(service, input);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
