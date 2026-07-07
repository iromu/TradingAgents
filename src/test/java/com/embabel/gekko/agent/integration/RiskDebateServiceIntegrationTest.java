package com.embabel.gekko.agent.integration;

import com.embabel.gekko.agent.FakeActionContext;
import com.embabel.gekko.agent.RiskAssessmentOutput;
import com.embabel.gekko.agent.RiskDebateAgent;
import com.embabel.gekko.agent.RiskLevel;
import com.embabel.gekko.agent.risk.AggressiveDebator;
import com.embabel.gekko.agent.risk.ConservativeDebator;
import com.embabel.gekko.agent.risk.NeutralDebator;
import com.embabel.gekko.domain.ResearchTypes;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for the RiskDebateService (RiskDebateAgent).
 *
 * Validates that the risk debate:
 * 1. Runs 3 rounds with 3 debators (9 total debator calls)
 * 2. Judges the debate with a structured risk assessment
 * 3. Returns the correct risk level
 */
@Tag("integration")
class RiskDebateServiceIntegrationTest {

    private RiskDebateAgent createAgent() {
        var aggressive = Mockito.mock(AggressiveDebator.class);
        var conservative = Mockito.mock(ConservativeDebator.class);
        var neutral = Mockito.mock(NeutralDebator.class);

        // Stub with any() matchers (matching the existing test pattern)
        when(aggressive.argue(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                        Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn("Aggressive argument");
        when(conservative.argue(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                        Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn("Conservative argument");
        when(neutral.argue(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                        Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn("Neutral argument");

        var aggressiveProvider = Mockito.mock(org.springframework.beans.factory.ObjectProvider.class);
        var conservativeProvider = Mockito.mock(org.springframework.beans.factory.ObjectProvider.class);
        var neutralProvider = Mockito.mock(org.springframework.beans.factory.ObjectProvider.class);
        when(aggressiveProvider.getObject()).thenReturn(aggressive);
        when(conservativeProvider.getObject()).thenReturn(conservative);
        when(neutralProvider.getObject()).thenReturn(neutral);

        return new RiskDebateAgent(aggressiveProvider, conservativeProvider, neutralProvider);
    }

    @Test
    void shouldAssessRiskWithStubbedResponses() {
        // Arrange
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var agent = createAgent();

        // Stub the 9 debator responses and the judge's structured output
        // The judge makes 2 LLM calls (RiskAssessmentOutput attempt + String fallback)
        // Total: 9 debator stubs + 1 judge stub = 10 stubs, but only 2 LLM invocations
        // (debators are mocked, so they don't make LLM calls)
        for (int i = 0; i < 9; i++) {
            fake.getDelegate().expectResponse("buy because of high risk appetite");
        }
        fake.getDelegate().expectResponse(new RiskAssessmentOutput(RiskLevel.RISKY, "buy because of high risk appetite"));

        var ticker = new ResearchTypes.Ticker("AAPL", "");
        var briefs = new ResearchTypes.DebateBriefs("F", "M", "N", "S");
        var debateState = new ResearchTypes.InvestmentDebateState(
                List.of(), List.of(), List.of(), "", 0, briefs
        );

        // Act
        var result = agent.assessRisk(ticker, briefs, debateState, "Invest", context);

        // Assert
        assertEquals(RiskLevel.RISKY, result.level());
        // 2 LLM invocations: judge's RiskAssessmentOutput attempt + String fallback
        assertEquals(2, fake.getDelegate().getPromptRunner().getLlmInvocations().size());
    }

    @Test
    void shouldVerifyAllDebatorInteractionIds() {
        // Arrange
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var agent = createAgent();

        for (int i = 0; i < 9; i++) {
            fake.getDelegate().expectResponse("buy because of high risk appetite");
        }
        fake.getDelegate().expectResponse(new RiskAssessmentOutput(RiskLevel.RISKY, "buy because of high risk appetite"));

        var ticker = new ResearchTypes.Ticker("AAPL", "");
        var briefs = new ResearchTypes.DebateBriefs("F", "M", "N", "S");
        var debateState = new ResearchTypes.InvestmentDebateState(
                List.of(), List.of(), List.of(), "", 0, briefs
        );

        // Act
        agent.assessRisk(ticker, briefs, debateState, "Invest", context);

        // Assert — verify the interaction IDs for each debator and the judge
        var invocations = fake.getDelegate().getPromptRunner().getLlmInvocations();

        // The debators are mocked, so they don't make LLM calls.
        // Only the judge makes LLM calls (2: RiskAssessmentOutput attempt + String fallback)
        assertEquals(2, invocations.size());
        assertEquals("riskJudge", invocations.get(0).getInteraction().getId());
        assertEquals("riskJudge", invocations.get(1).getInteraction().getId());
    }
}