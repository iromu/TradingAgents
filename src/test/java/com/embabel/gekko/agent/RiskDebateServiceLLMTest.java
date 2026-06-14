package com.embabel.gekko.agent;

import com.embabel.gekko.agent.FakeActionContext;
import com.embabel.gekko.domain.ResearchTypes;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RiskDebateAgent.assessRisk using FakePromptRunner.
 * Verifies 10 LLM invocations (3 rounds x 3 debators + 1 judge) with correct IDs.
 */
class RiskDebateServiceLLMTest {

    private RiskDebateAgent createAgent() {
        return new RiskDebateAgent(null);
    }

    @Test
    void assessRisk_10LLMCallsVerified() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var delegate = fake.getDelegate();
        for (int i = 0; i < 9; i++) {
            delegate.expectResponse("NEUTRAL");
        }
        // 10th response for structured judge output
        delegate.expectResponse(new RiskAssessmentOutput(RiskLevel.NEUTRAL, "NEUTRAL"));
        var ticker = new ResearchTypes.Ticker("AAPL", "");
        var briefs = new ResearchTypes.DebateBriefs("F", "M", "N", "S");
        var debateState = new ResearchTypes.InvestmentDebateState(
                List.of("history"), List.of(), List.of(), "", 0, briefs, null
        );
        createAgent().assessRisk(ticker, briefs, debateState, context);
        var invocations = delegate.getPromptRunner().getLlmInvocations();
        assertEquals(10, invocations.size());
        for (int i = 0; i < 9; i++) {
            assertEquals("riskDebator", invocations.get(i).getInteraction().getId());
        }
        assertEquals("riskJudge", invocations.get(9).getInteraction().getId());
    }

    @Test
    void assessRisk_parsesRiskyAssessment() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        for (int i = 0; i < 9; i++) {
            fake.getDelegate().expectResponse("buy because of high risk appetite");
        }
        fake.getDelegate().expectResponse(new RiskAssessmentOutput(RiskLevel.RISKY, "buy because of high risk appetite"));
        var ticker = new ResearchTypes.Ticker("TSLA", "");
        var briefs = new ResearchTypes.DebateBriefs("F", "M", "N", "S");
        var debateState = new ResearchTypes.InvestmentDebateState(
                List.of(), List.of(), List.of(), "", 0, briefs, null
        );
        var result = createAgent().assessRisk(ticker, briefs, debateState, context);
        assertEquals(RiskLevel.RISKY, result.level());
    }

    @Test
    void assessRisk_parsesConservativeAssessment() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        for (int i = 0; i < 9; i++) {
            fake.getDelegate().expectResponse("sell to avoid risk");
        }
        fake.getDelegate().expectResponse(new RiskAssessmentOutput(RiskLevel.CONSERVATIVE, "sell to avoid risk"));
        var ticker = new ResearchTypes.Ticker("KO", "");
        var briefs = new ResearchTypes.DebateBriefs("F", "M", "N", "S");
        var debateState = new ResearchTypes.InvestmentDebateState(
                List.of(), List.of(), List.of(), "", 0, briefs, null
        );
        var result = createAgent().assessRisk(ticker, briefs, debateState, context);
        assertEquals(RiskLevel.CONSERVATIVE, result.level());
    }

    @Test
    void assessRisk_parsesNeutralAssessment() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        for (int i = 0; i < 9; i++) {
            fake.getDelegate().expectResponse("hold position with moderate outlook");
        }
        fake.getDelegate().expectResponse(new RiskAssessmentOutput(RiskLevel.NEUTRAL, "hold position with moderate outlook"));
        var ticker = new ResearchTypes.Ticker("JNJ", "");
        var briefs = new ResearchTypes.DebateBriefs("F", "M", "N", "S");
        var debateState = new ResearchTypes.InvestmentDebateState(
                List.of(), List.of(), List.of(), "", 0, briefs, null
        );
        var result = createAgent().assessRisk(ticker, briefs, debateState, context);
        assertEquals(RiskLevel.NEUTRAL, result.level());
    }

    @Test
    void assessRisk_handlesEmptyDebateHistory() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        for (int i = 0; i < 9; i++) {
            fake.getDelegate().expectResponse("NEUTRAL");
        }
        fake.getDelegate().expectResponse(new RiskAssessmentOutput(RiskLevel.NEUTRAL, "no debate history available"));
        var ticker = new ResearchTypes.Ticker("AAPL", "");
        var briefs = new ResearchTypes.DebateBriefs("F", "M", "N", "S");
        var debateState = new ResearchTypes.InvestmentDebateState(
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                "", 0, briefs, null
        );
        var result = createAgent().assessRisk(ticker, briefs, debateState, context);
        assertNotNull(result);
        assertEquals(RiskLevel.NEUTRAL, result.level());
    }

    @Test
    void assessRisk_handlesEmptyBriefs() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        for (int i = 0; i < 9; i++) {
            fake.getDelegate().expectResponse("NEUTRAL");
        }
        fake.getDelegate().expectResponse(new RiskAssessmentOutput(RiskLevel.NEUTRAL, "NEUTRAL"));
        var ticker = new ResearchTypes.Ticker("AAPL", "");
        var briefs = new ResearchTypes.DebateBriefs("", "", "", "");
        var debateState = new ResearchTypes.InvestmentDebateState(
                List.of(), List.of(), List.of(), "", 0, briefs, null
        );
        var result = createAgent().assessRisk(ticker, briefs, debateState, context);
        assertNotNull(result);
    }

    @Test
    void assessRisk_allInvocationsRecordedOnSameRunner() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var delegate = fake.getDelegate();
        for (int i = 0; i < 9; i++) {
            delegate.expectResponse("NEUTRAL");
        }
        delegate.expectResponse(new RiskAssessmentOutput(RiskLevel.NEUTRAL, "NEUTRAL"));
        var ticker = new ResearchTypes.Ticker("AAPL", "");
        var briefs = new ResearchTypes.DebateBriefs("F", "M", "N", "S");
        var debateState = new ResearchTypes.InvestmentDebateState(
                List.of(), List.of(), List.of(), "", 0, briefs, null
        );
        var promptRunner = delegate.getPromptRunner();
        createAgent().assessRisk(ticker, briefs, debateState, context);
        var invocations = promptRunner.getLlmInvocations();
        assertEquals(10, invocations.size());
        assertSame(invocations, delegate.getLlmInvocations());
    }
}
