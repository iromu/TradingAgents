package com.embabel.gekko.agent;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RiskDebateAgent.parseRiskAssessmentFallback.
 * After task 2.7, the fallback always returns NEUTRAL (no keyword-based classification)
 * because speaker labels "Aggressive"/"Conservative" in the transcript caused false positives.
 */
class RiskDebateServiceUnitTest {

    private RiskDebateAgent createAgent() {
        return new RiskDebateAgent(
                null,
                Mockito.mock(org.springframework.beans.factory.ObjectProvider.class),
                Mockito.mock(org.springframework.beans.factory.ObjectProvider.class),
                Mockito.mock(org.springframework.beans.factory.ObjectProvider.class),
                null
        );
    }

    @Test
    void parseRiskAssessment_risky_returnsNeutral() {
        var agent = createAgent();
        var result = invokeParseRiskAssessment(agent, "buy because of high risk appetite");
        assertEquals(RiskLevel.NEUTRAL, result.level());
        assertTrue(result.reasoning().contains("undetermined"));
    }

    @Test
    void parseRiskAssessment_risky_bold_returnsNeutral() {
        var agent = createAgent();
        var result = invokeParseRiskAssessment(agent, "bold buy aggressive move");
        assertEquals(RiskLevel.NEUTRAL, result.level());
    }

    @Test
    void parseRiskAssessment_conservative_sell_returnsNeutral() {
        var agent = createAgent();
        var result = invokeParseRiskAssessment(agent, "sell to avoid risk");
        assertEquals(RiskLevel.NEUTRAL, result.level());
    }

    @Test
    void parseRiskAssessment_conservative_cautious_returnsNeutral() {
        var agent = createAgent();
        var result = invokeParseRiskAssessment(agent, "cautious approach recommended");
        assertEquals(RiskLevel.NEUTRAL, result.level());
    }

    @Test
    void parseRiskAssessment_conservative_safe_returnsNeutral() {
        var agent = createAgent();
        var result = invokeParseRiskAssessment(agent, "safe investment strategy");
        assertEquals(RiskLevel.NEUTRAL, result.level());
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
    }

    @Test
    void parseRiskAssessment_neutral_null() {
        var agent = createAgent();
        var result = invokeParseRiskAssessment(agent, null);
        assertEquals(RiskLevel.NEUTRAL, result.level());
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
        // Reasoning includes the truncated judge text
        assertNotNull(result.reasoning());
    }

    @Test
    void parseRiskAssessment_risky_high_returnsNeutral() {
        var agent = createAgent();
        var result = invokeParseRiskAssessment(agent, "buy high risk position");
        assertEquals(RiskLevel.NEUTRAL, result.level());
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
