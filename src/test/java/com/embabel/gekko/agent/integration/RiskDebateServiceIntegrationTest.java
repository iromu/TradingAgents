package com.embabel.gekko.agent.integration;

import com.embabel.agent.test.integration.EmbabelMockitoIntegrationTest;
import com.embabel.gekko.agent.FakeActionContext;
import com.embabel.gekko.agent.RiskAssessment;
import com.embabel.gekko.agent.RiskAssessmentOutput;
import com.embabel.gekko.agent.RiskDebateAgent;
import com.embabel.gekko.agent.RiskLevel;
import com.embabel.gekko.domain.ResearchTypes;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the RiskDebateService (RiskDebateAgent).
 *
 * Validates that the risk debate:
 * 1. Runs 3 rounds with 3 debators (9 total debator calls)
 * 2. Judges the debate with a structured risk assessment
 * 3. Returns the correct risk level
 *
 * Extends EmbabelMockitoIntegrationTest for Spring context access.
 * Uses real debator beans from Spring (not Mockito mocks) — LLM calls go through FakeActionContext.
 */
@Tag("integration")
class RiskDebateServiceIntegrationTest extends EmbabelMockitoIntegrationTest {

    @Autowired
    private RiskDebateAgent riskDebateAgent;

    @Test
    void shouldAssessRiskWithStubbedResponses() {
        var fake = FakeActionContext.create();

        // Stub the 9 debator responses (3 rounds × 3 debators)
        // Each debator uses createObject(String.class, ...) — same response for all
        for (int i = 0; i < 9; i++) {
            fake.getDelegate().expectResponse("debate argument");
        }
        // Stub the judge's structured output attempt (RiskAssessmentOutput.class)
        // The judge tries RiskAssessmentOutput first, then falls back to String
        fake.getDelegate().expectResponse(new RiskAssessmentOutput(RiskLevel.RISKY, "buy because of high risk appetite"));

        // Act
        var ticker = new ResearchTypes.Ticker("AAPL", "");
        var briefs = new ResearchTypes.DebateBriefs("F", "M", "N", "S");
        var debateState = new ResearchTypes.InvestmentDebateState(
                List.of(), List.of(), List.of(), "", 0, briefs
        );

        var result = riskDebateAgent.assessRisk(ticker, briefs, debateState, "Invest", fake.getActionContext());

        // Assert
        assertEquals(RiskLevel.RISKY, result.level());
        assertEquals("buy because of high risk appetite", result.reasoning());

        // Verify the LLM interactions:
        // 9 debator calls + 1 judge call (RiskAssessmentOutput) = 10 total
        // (The judge's fallback String call is not consumed because the RiskAssessmentOutput stub succeeds)
        var invocations = fake.getDelegate().getPromptRunner().getLlmInvocations();
        assertEquals(10, invocations.size());
    }

    @Test
    void shouldVerifyAllDebatorInteractionIds() {
        var fake = FakeActionContext.create();

        // Stub the 9 debator responses (3 rounds × 3 debators)
        for (int i = 0; i < 9; i++) {
            fake.getDelegate().expectResponse("debate argument");
        }
        // Stub the judge's structured output attempt
        fake.getDelegate().expectResponse(new RiskAssessmentOutput(RiskLevel.RISKY, "buy because of high risk appetite"));

        // Act
        var ticker = new ResearchTypes.Ticker("AAPL", "");
        var briefs = new ResearchTypes.DebateBriefs("F", "M", "N", "S");
        var debateState = new ResearchTypes.InvestmentDebateState(
                List.of(), List.of(), List.of(), "", 0, briefs
        );

        riskDebateAgent.assessRisk(ticker, briefs, debateState, "Invest", fake.getActionContext());

        // Assert — verify the interaction IDs for each debator and the judge
        var invocations = fake.getDelegate().getPromptRunner().getLlmInvocations();
        assertEquals(10, invocations.size());

        // Verify debator interaction IDs: 3 rounds × 3 debators = 9 invocations
        // Round 1: aggressiveDebator, conservativeDebator, neutralDebator
        // Round 2: aggressiveDebator, conservativeDebator, neutralDebator
        // Round 3: aggressiveDebator, conservativeDebator, neutralDebator
        var expectedDebatorIds = List.of(
                "aggressiveDebator", "conservativeDebator", "neutralDebator",
                "aggressiveDebator", "conservativeDebator", "neutralDebator",
                "aggressiveDebator", "conservativeDebator", "neutralDebator"
        );
        for (int i = 0; i < 9; i++) {
            assertEquals(expectedDebatorIds.get(i), invocations.get(i).getInteraction().getId(),
                    "Debator at index " + i + " should have id " + expectedDebatorIds.get(i));
        }

        // Verify the judge interaction ID
        assertEquals("riskJudge", invocations.get(9).getInteraction().getId());
    }
}