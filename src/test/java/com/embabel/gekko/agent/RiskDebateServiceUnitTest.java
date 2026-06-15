package com.embabel.gekko.agent;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RiskDebateAgent.parseRiskAssessment covering all 3 risk levels.
 */
class RiskDebateServiceUnitTest {

    private RiskDebateAgent createAgent() {
        return new RiskDebateAgent(
                Mockito.mock(org.springframework.beans.factory.ObjectProvider.class),
                Mockito.mock(org.springframework.beans.factory.ObjectProvider.class),
                Mockito.mock(org.springframework.beans.factory.ObjectProvider.class)
        );
    }

    @Test
    void parseRiskAssessment_risky() {
        var agent = createAgent();
        var result = invokeParseRiskAssessment(agent, "buy because of high risk appetite");
        assertEquals(RiskLevel.RISKY, result.level());
        assertTrue(result.reasoning().toLowerCase().contains("buy"));
    }

    @Test
    void parseRiskAssessment_risky_bold() {
        var agent = createAgent();
        var result = invokeParseRiskAssessment(agent, "bold buy aggressive move");
        assertEquals(RiskLevel.RISKY, result.level());
    }

    @Test
    void parseRiskAssessment_conservative_sell() {
        var agent = createAgent();
        var result = invokeParseRiskAssessment(agent, "sell to avoid risk");
        assertEquals(RiskLevel.CONSERVATIVE, result.level());
    }

    @Test
    void parseRiskAssessment_conservative_cautious() {
        var agent = createAgent();
        var result = invokeParseRiskAssessment(agent, "cautious approach recommended");
        assertEquals(RiskLevel.CONSERVATIVE, result.level());
    }

    @Test
    void parseRiskAssessment_conservative_safe() {
        var agent = createAgent();
        var result = invokeParseRiskAssessment(agent, "safe investment strategy");
        assertEquals(RiskLevel.CONSERVATIVE, result.level());
    }

    @Test
    void parseRiskAssessment_neutral_default() {
        var agent = createAgent();
        var result = invokeParseRiskAssessment(agent, "hold position with moderate outlook");
        assertEquals(RiskLevel.NEUTRAL, result.level());
    }

    @Test
    void parseRiskAssessment_neutral_empty() {
        var agent = createAgent();
        var result = invokeParseRiskAssessment(agent, "");
        assertEquals(RiskLevel.NEUTRAL, result.level());
        assertTrue(result.reasoning().contains("empty"));
    }

    @Test
    void parseRiskAssessment_neutral_null() {
        var agent = createAgent();
        var result = invokeParseRiskAssessment(agent, null);
        assertEquals(RiskLevel.NEUTRAL, result.level());
        assertTrue(result.reasoning().contains("empty"));
    }

    @Test
    void parseRiskAssessment_reasoning_truncated() {
        var agent = createAgent();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 250; i++) {
            sb.append("a");
        }
        var result = invokeParseRiskAssessment(agent, sb.toString());
        assertEquals(RiskLevel.NEUTRAL, result.level());
        // Reasoning is truncated to 200 chars + "..." = 203 chars max
        assertTrue(result.reasoning().length() <= 203);
    }

    @Test
    void parseRiskAssessment_risky_high() {
        var agent = createAgent();
        var result = invokeParseRiskAssessment(agent, "buy high risk position");
        assertEquals(RiskLevel.RISKY, result.level());
    }

    private RiskAssessment invokeParseRiskAssessment(RiskDebateAgent agent, String input) {
        try {
            var method = RiskDebateAgent.class.getDeclaredMethod("parseRiskAssessmentFallback", String.class);
            method.setAccessible(true);
            return (RiskAssessment) method.invoke(agent, input);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
