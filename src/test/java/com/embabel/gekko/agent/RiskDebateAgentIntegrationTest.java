package com.embabel.gekko.agent;

import com.embabel.gekko.agent.risk.AggressiveDebator;
import com.embabel.gekko.agent.risk.ConservativeDebator;
import com.embabel.gekko.agent.risk.NeutralDebator;
import com.embabel.gekko.domain.ResearchTypes;
import org.mockito.Mockito;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for RiskDebateAgent.assessRisk().
 * Uses real debator instances (like RiskDebateServiceLLMTest) so that
 * LLM calls through FakePromptRunner consume expectResponse stubs correctly.
 */
class RiskDebateAgentIntegrationTest {

    private static ResearchTypes.Ticker ticker() {
        return new ResearchTypes.Ticker("AAPL", "");
    }

    private static ResearchTypes.DebateBriefs briefs() {
        return new ResearchTypes.DebateBriefs(
                "Fundamentals: strong revenue growth",
                "Market: bullish breakout",
                "News: positive product launch",
                "Social: strong sentiment"
        );
    }

    private static ResearchTypes.InvestmentDebateState debateState() {
        return new ResearchTypes.InvestmentDebateState(
                List.of("bull argument", "bear argument"),
                List.of("bull argument"),
                List.of("bear argument"),
                "bear argument", 2,
                briefs()
        );
    }

    private static RiskDebateAgent createAgent() {
        var aggressive = new AggressiveDebator();
        var conservative = new ConservativeDebator();
        var neutral = new NeutralDebator();

        var aggressiveProvider = Mockito.mock(org.springframework.beans.factory.ObjectProvider.class);
        var conservativeProvider = Mockito.mock(org.springframework.beans.factory.ObjectProvider.class);
        var neutralProvider = Mockito.mock(org.springframework.beans.factory.ObjectProvider.class);
        Mockito.when(aggressiveProvider.getObject()).thenReturn(aggressive);
        Mockito.when(conservativeProvider.getObject()).thenReturn(conservative);
        Mockito.when(neutralProvider.getObject()).thenReturn(neutral);

        return new RiskDebateAgent(aggressiveProvider, conservativeProvider, neutralProvider, null);
    }

    @Test
    void assessRisk_invokesAll3Debators3TimesEach() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var delegate = fake.getDelegate();

        // 9 debator LLM calls + 1 judge LLM call = 10 total
        for (int i = 0; i < 9; i++) {
            delegate.expectResponse("Debator response " + i);
        }
        delegate.expectResponse(new RiskAssessmentOutput(RiskLevel.RISKY, "Moderate risk with balanced outlook"));

        var agent = createAgent();
        var result = agent.assessRisk(ticker(), briefs(), debateState(), "Buy AAPL", context);

        assertEquals(RiskLevel.RISKY, result.level());
        assertEquals(10, delegate.getPromptRunner().getLlmInvocations().size());
    }

    @Test
    void assessRisk_producesValidRiskAssessment() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var delegate = fake.getDelegate();

        for (int i = 0; i < 9; i++) {
            delegate.expectResponse("Response " + i);
        }
        delegate.expectResponse(new RiskAssessmentOutput(RiskLevel.NEUTRAL, "balanced risk profile"));

        var agent = createAgent();
        var result = agent.assessRisk(ticker(), briefs(), debateState(), "Buy AAPL", context);

        assertNotNull(result);
        assertNotNull(result.level());
        assertNotNull(result.reasoning());
        assertFalse(result.reasoning().isBlank());
    }

    @Test
    void assessRisk_riskyClassification() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var delegate = fake.getDelegate();

        for (int i = 0; i < 9; i++) {
            delegate.expectResponse("buy because of high risk appetite");
        }
        delegate.expectResponse(new RiskAssessmentOutput(RiskLevel.RISKY, "aggressive growth with high risk tolerance"));

        var agent = createAgent();
        var result = agent.assessRisk(ticker(), briefs(), debateState(), "Buy AAPL", context);

        assertEquals(RiskLevel.RISKY, result.level());
    }

    @Test
    void assessRisk_conservativeClassification() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var delegate = fake.getDelegate();

        for (int i = 0; i < 9; i++) {
            delegate.expectResponse("sell to avoid risk");
        }
        delegate.expectResponse(new RiskAssessmentOutput(RiskLevel.CONSERVATIVE, "avoid this stock, too cautious approach needed"));

        var agent = createAgent();
        var result = agent.assessRisk(ticker(), briefs(), debateState(), "Buy AAPL", context);

        assertEquals(RiskLevel.CONSERVATIVE, result.level());
    }

    @Test
    void assessRisk_neutralDefault() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var delegate = fake.getDelegate();

        for (int i = 0; i < 9; i++) {
            delegate.expectResponse("Response " + i);
        }
        delegate.expectResponse(new RiskAssessmentOutput(RiskLevel.NEUTRAL, "moderate fundamentals with steady growth"));

        var agent = createAgent();
        var result = agent.assessRisk(ticker(), briefs(), debateState(), "Buy AAPL", context);

        assertEquals(RiskLevel.NEUTRAL, result.level());
    }

    @Test
    void assessRisk_judgeUsesBESTRole() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var delegate = fake.getDelegate();

        for (int i = 0; i < 9; i++) {
            delegate.expectResponse("Response " + i);
        }
        delegate.expectResponse(new RiskAssessmentOutput(RiskLevel.NEUTRAL, "balanced"));

        var agent = createAgent();
        agent.assessRisk(ticker(), briefs(), debateState(), "Buy AAPL", context);

        var invocations = delegate.getPromptRunner().getLlmInvocations();
        var lastInvocation = invocations.get(invocations.size() - 1);
        assertEquals("riskJudge", lastInvocation.getInteraction().getId());
        assertEquals("best", lastInvocation.getInteraction().getLlm().getRole());
    }

    @Test
    void assessRisk_includesTraderProposalInJudgePrompt() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var delegate = fake.getDelegate();

        for (int i = 0; i < 9; i++) {
            delegate.expectResponse("Response " + i);
        }
        delegate.expectResponse(new RiskAssessmentOutput(RiskLevel.NEUTRAL, "balanced"));

        var agent = createAgent();
        agent.assessRisk(ticker(), briefs(), debateState(), "Buy 50% of AAPL at $150 with stop-loss at $140", context);

        var invocations = delegate.getPromptRunner().getLlmInvocations();
        var lastInvocation = invocations.get(invocations.size() - 1);
        assertTrue(lastInvocation.getPrompt().contains("Buy 50% of AAPL at $150"));
    }
}