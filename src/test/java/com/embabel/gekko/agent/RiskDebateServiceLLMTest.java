package com.embabel.gekko.agent;

import com.embabel.gekko.agent.TraderAgent.DebateBriefs;
import com.embabel.gekko.agent.TraderAgent.InvestmentDebateState;
import com.embabel.gekko.agent.TraderAgent.Ticker;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RiskDebateService.runRiskDebate using FakePromptRunner.
 * Verifies 10 LLM invocations (3 rounds x 3 debators + 1 judge) with correct IDs.
 */
class RiskDebateServiceLLMTest {

    private RiskDebateService createService() {
        return new RiskDebateService();
    }

    @Test
    void runRiskDebate_10LLMCallsVerified() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var delegate = fake.getDelegate();
        for (int i = 0; i < 10; i++) {
            delegate.expectResponse("NEUTRAL");
        }
        var ticker = new Ticker("AAPL", "");
        var briefs = new DebateBriefs("F", "M", "N", "S");
        var debateState = new InvestmentDebateState(
                List.of("history"), List.of(), List.of(), "", 0, briefs, null
        );
        createService().runRiskDebate(ticker, briefs, debateState, context);
        var invocations = delegate.getPromptRunner().getLlmInvocations();
        assertEquals(10, invocations.size());
        for (int i = 0; i < 9; i++) {
            assertEquals("riskDebator", invocations.get(i).getInteraction().getId());
        }
        assertEquals("riskJudge", invocations.get(9).getInteraction().getId());
    }

    @Test
    void runRiskDebate_parsesRiskyAssessment() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        for (int i = 0; i < 10; i++) {
            fake.getDelegate().expectResponse("buy because of high risk appetite");
        }
        var ticker = new Ticker("TSLA", "");
        var briefs = new DebateBriefs("F", "M", "N", "S");
        var debateState = new InvestmentDebateState(
                List.of(), List.of(), List.of(), "", 0, briefs, null
        );
        var result = createService().runRiskDebate(ticker, briefs, debateState, context);
        assertEquals(RiskLevel.RISKY, result.level());
    }

    @Test
    void runRiskDebate_parsesConservativeAssessment() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        for (int i = 0; i < 10; i++) {
            fake.getDelegate().expectResponse("sell to avoid risk");
        }
        var ticker = new Ticker("KO", "");
        var briefs = new DebateBriefs("F", "M", "N", "S");
        var debateState = new InvestmentDebateState(
                List.of(), List.of(), List.of(), "", 0, briefs, null
        );
        var result = createService().runRiskDebate(ticker, briefs, debateState, context);
        assertEquals(RiskLevel.CONSERVATIVE, result.level());
    }

    @Test
    void runRiskDebate_parsesNeutralAssessment() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        for (int i = 0; i < 10; i++) {
            fake.getDelegate().expectResponse("hold position with moderate outlook");
        }
        var ticker = new Ticker("JNJ", "");
        var briefs = new DebateBriefs("F", "M", "N", "S");
        var debateState = new InvestmentDebateState(
                List.of(), List.of(), List.of(), "", 0, briefs, null
        );
        var result = createService().runRiskDebate(ticker, briefs, debateState, context);
        assertEquals(RiskLevel.NEUTRAL, result.level());
    }

    @Test
    void runRiskDebate_handlesEmptyDebateHistory() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        for (int i = 0; i < 10; i++) {
            fake.getDelegate().expectResponse("NEUTRAL");
        }
        var ticker = new Ticker("AAPL", "");
        var briefs = new DebateBriefs("F", "M", "N", "S");
        var debateState = new InvestmentDebateState(
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                "", 0, briefs, null
        );
        var result = createService().runRiskDebate(ticker, briefs, debateState, context);
        assertNotNull(result);
        assertEquals(RiskLevel.NEUTRAL, result.level());
    }

    @Test
    void runRiskDebate_handlesEmptyBriefs() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        for (int i = 0; i < 10; i++) {
            fake.getDelegate().expectResponse("NEUTRAL");
        }
        var ticker = new Ticker("AAPL", "");
        var briefs = new DebateBriefs("", "", "", "");
        var debateState = new InvestmentDebateState(
                List.of(), List.of(), List.of(), "", 0, briefs, null
        );
        var result = createService().runRiskDebate(ticker, briefs, debateState, context);
        assertNotNull(result);
    }

    @Test
    void runRiskDebate_allInvocationsRecordedOnSameRunner() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var delegate = fake.getDelegate();
        for (int i = 0; i < 10; i++) {
            delegate.expectResponse("NEUTRAL");
        }
        var ticker = new Ticker("AAPL", "");
        var briefs = new DebateBriefs("F", "M", "N", "S");
        var debateState = new InvestmentDebateState(
                List.of(), List.of(), List.of(), "", 0, briefs, null
        );
        var promptRunner = delegate.getPromptRunner();
        createService().runRiskDebate(ticker, briefs, debateState, context);
        var invocations = promptRunner.getLlmInvocations();
        assertEquals(10, invocations.size());
        assertSame(invocations, delegate.getLlmInvocations());
    }
}
