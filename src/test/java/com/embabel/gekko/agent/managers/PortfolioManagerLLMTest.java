package com.embabel.gekko.agent.managers;

import com.embabel.gekko.agent.FakeActionContext;
import com.embabel.gekko.agent.RiskAssessment;
import com.embabel.gekko.agent.RiskLevel;
import com.embabel.gekko.domain.ResearchTypes;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PortfolioManager using FakePromptRunner.
 * Verifies BEST_ROLE, interaction ID, and prompt content.
 */
class PortfolioManagerLLMTest {

    private static ResearchTypes.Ticker ticker() {
        return new ResearchTypes.Ticker("AAPL", "");
    }

    private static ResearchTypes.InvestmentDebateState debateState() {
        return new ResearchTypes.InvestmentDebateState(
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                "",
                0,
                null
        );
    }

    private static RiskAssessment riskAssessment(RiskLevel level) {
        return new RiskAssessment(level, "Test reasoning");
    }

    @Test
    void portfolioDecision_usesBEST_ROLE() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var delegate = fake.getDelegate();
        delegate.expectResponse("BUY 50% of AAPL shares.");
        delegate.expectResponse("BUY 50% of AAPL shares.");
        var portfolioManager = new PortfolioManager();
        portfolioManager.portfolioDecision(
                ticker(), debateState(), "Research plan content", "Trader proposal",
                riskAssessment(RiskLevel.NEUTRAL), context
        );
        var invocations = delegate.getPromptRunner().getLlmInvocations();
        assertEquals(2, invocations.size());
        assertEquals("best", invocations.get(0).getInteraction().getLlm().getRole());
    }

    @Test
    void portfolioDecision_usesCorrectInteractionId() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var delegate = fake.getDelegate();
        delegate.expectResponse("BUY 50% of AAPL shares.");
        delegate.expectResponse("BUY 50% of AAPL shares.");
        var portfolioManager = new PortfolioManager();
        portfolioManager.portfolioDecision(
                ticker(), debateState(), "Research plan content", "Trader proposal",
                riskAssessment(RiskLevel.NEUTRAL), context
        );
        var invocations = delegate.getPromptRunner().getLlmInvocations();
        assertEquals(2, invocations.size());
        assertEquals("portfolioManager", invocations.get(0).getInteraction().getId());
    }

    @Test
    void portfolioDecision_promptContainsTicker() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var delegate = fake.getDelegate();
        delegate.expectResponse("BUY 50% of AAPL shares.");
        delegate.expectResponse("BUY 50% of AAPL shares.");
        var portfolioManager = new PortfolioManager();
        portfolioManager.portfolioDecision(
                ticker(), debateState(), "Research plan content", "Trader proposal",
                riskAssessment(RiskLevel.NEUTRAL), context
        );
        var prompt = delegate.getPromptRunner().getLlmInvocations().get(0).getPrompt();
        assertTrue(prompt.contains("AAPL"));
    }

    @Test
    void portfolioDecision_promptContainsResearchPlan() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var delegate = fake.getDelegate();
        delegate.expectResponse("BUY 50% of AAPL shares.");
        delegate.expectResponse("BUY 50% of AAPL shares.");
        var portfolioManager = new PortfolioManager();
        portfolioManager.portfolioDecision(
                ticker(), debateState(), "Detailed research plan for AAPL.", "Trader proposal",
                riskAssessment(RiskLevel.NEUTRAL), context
        );
        var prompt = delegate.getPromptRunner().getLlmInvocations().get(0).getPrompt();
        assertTrue(prompt.contains("Detailed research plan for AAPL"));
    }

    @Test
    void portfolioDecision_promptContainsRiskLevel() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var delegate = fake.getDelegate();
        delegate.expectResponse("BUY 50% of AAPL shares.");
        delegate.expectResponse("BUY 50% of AAPL shares.");
        var portfolioManager = new PortfolioManager();
        portfolioManager.portfolioDecision(
                ticker(), debateState(), "Research plan content", "Trader proposal",
                riskAssessment(RiskLevel.RISKY), context
        );
        var prompt = delegate.getPromptRunner().getLlmInvocations().get(0).getPrompt();
        assertTrue(prompt.contains("RISKY"));
    }
}