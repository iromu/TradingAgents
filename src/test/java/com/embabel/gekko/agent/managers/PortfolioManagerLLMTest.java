package com.embabel.gekko.agent.managers;

import com.embabel.gekko.agent.FakeActionContext;
import com.embabel.gekko.domain.PortfolioDecisionOutput;
import com.embabel.gekko.agent.RiskAssessment;
import com.embabel.gekko.agent.RiskLevel;
import com.embabel.gekko.domain.PortfolioRating;
import com.embabel.gekko.domain.ResearchTypes;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LLM interaction tests for PortfolioManager.portfolioDecision().
 * Verifies: BEST_ROLE, interaction ID, prompt content, and structured output fallback.
 */
class PortfolioManagerLLMTest {

    private static ResearchTypes.Ticker ticker() {
        return new ResearchTypes.Ticker("AAPL", "");
    }

    private static ResearchTypes.InvestmentDebateState debateState() {
        return new ResearchTypes.InvestmentDebateState(
                List.of("Bull argument: strong growth", "Bear argument: valuation concern"),
                List.of("Bull argument: strong growth"),
                List.of("Bear argument: valuation concern"),
                "Bear argument: valuation concern", 2,
                new ResearchTypes.DebateBriefs("Fundamentals brief", "Market brief", "News brief", "Social brief")
        );
    }

    @Test
    void portfolioDecision_usesBestRole() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var delegate = fake.getDelegate();
        delegate.expectResponse(new PortfolioDecisionOutput(PortfolioRating.BUY, "Summary", "Thesis", null, null));
        delegate.expectResponse(new PortfolioDecisionOutput(PortfolioRating.BUY, "Summary", "Thesis", null, null));

        var portfolioManager = new PortfolioManager();
        portfolioManager.portfolioDecision(
                ticker(), debateState(),
                "AAPL: Strong fundamentals, buy at $150.",
                "Buy 50% at $150",
                new RiskAssessment(RiskLevel.NEUTRAL, "Moderate"),
                context
        );

        var invocations = delegate.getPromptRunner().getLlmInvocations();
        assertEquals(1, invocations.size());
        for (var invocation : invocations) {
            assertEquals("best", invocation.getInteraction().getLlm().getRole());
        }
    }

    @Test
    void portfolioDecision_usesCorrectInteractionId() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var delegate = fake.getDelegate();
        delegate.expectResponse(new PortfolioDecisionOutput(PortfolioRating.BUY, "Summary", "Thesis", null, null));
        delegate.expectResponse(new PortfolioDecisionOutput(PortfolioRating.BUY, "Summary", "Thesis", null, null));

        var portfolioManager = new PortfolioManager();
        portfolioManager.portfolioDecision(
                ticker(), debateState(),
                "AAPL: Strong fundamentals, buy at $150.",
                "Buy 50% at $150",
                new RiskAssessment(RiskLevel.NEUTRAL, "Moderate"),
                context
        );

        var invocations = delegate.getPromptRunner().getLlmInvocations();
        for (var invocation : invocations) {
            assertEquals("portfolioManager", invocation.getInteraction().getId());
        }
    }

    @Test
    void portfolioDecision_promptContainsTicker() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var delegate = fake.getDelegate();
        delegate.expectResponse(new PortfolioDecisionOutput(PortfolioRating.BUY, "Summary", "Thesis", null, null));
        delegate.expectResponse(new PortfolioDecisionOutput(PortfolioRating.BUY, "Summary", "Thesis", null, null));

        var portfolioManager = new PortfolioManager();
        portfolioManager.portfolioDecision(
                ticker(), debateState(),
                "AAPL: Strong fundamentals, buy at $150.",
                "Buy 50% at $150",
                new RiskAssessment(RiskLevel.NEUTRAL, "Moderate"),
                context
        );

        var prompt = delegate.getPromptRunner().getLlmInvocations().get(0).getPrompt();
        assertTrue(prompt.contains("AAPL"));
    }

    @Test
    void portfolioDecision_promptContainsDebateState() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var delegate = fake.getDelegate();
        delegate.expectResponse(new PortfolioDecisionOutput(PortfolioRating.BUY, "Summary", "Thesis", null, null));
        delegate.expectResponse(new PortfolioDecisionOutput(PortfolioRating.BUY, "Summary", "Thesis", null, null));

        var portfolioManager = new PortfolioManager();
        portfolioManager.portfolioDecision(
                ticker(), debateState(),
                "AAPL: Strong fundamentals, buy at $150.",
                "Buy 50% at $150",
                new RiskAssessment(RiskLevel.NEUTRAL, "Moderate"),
                context
        );

        var prompt = delegate.getPromptRunner().getLlmInvocations().get(0).getPrompt();
        assertTrue(prompt.contains("strong growth"));
        assertTrue(prompt.contains("valuation concern"));
    }

    @Test
    void portfolioDecision_promptContainsResearchPlan() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var delegate = fake.getDelegate();
        delegate.expectResponse(new PortfolioDecisionOutput(PortfolioRating.BUY, "Summary", "Thesis", null, null));
        delegate.expectResponse(new PortfolioDecisionOutput(PortfolioRating.BUY, "Summary", "Thesis", null, null));

        var portfolioManager = new PortfolioManager();
        portfolioManager.portfolioDecision(
                ticker(), debateState(),
                "AAPL: Strong fundamentals, buy at $150.",
                "Buy 50% at $150",
                new RiskAssessment(RiskLevel.NEUTRAL, "Moderate"),
                context
        );

        var prompt = delegate.getPromptRunner().getLlmInvocations().get(0).getPrompt();
        assertTrue(prompt.contains("Strong fundamentals"));
        assertTrue(prompt.contains("buy at $150"));
    }

    @Test
    void portfolioDecision_promptContainsTraderProposal() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var delegate = fake.getDelegate();
        delegate.expectResponse(new PortfolioDecisionOutput(PortfolioRating.BUY, "Summary", "Thesis", null, null));
        delegate.expectResponse(new PortfolioDecisionOutput(PortfolioRating.BUY, "Summary", "Thesis", null, null));

        var portfolioManager = new PortfolioManager();
        portfolioManager.portfolioDecision(
                ticker(), debateState(),
                "AAPL: Strong fundamentals, buy at $150.",
                "Buy 50% at $150",
                new RiskAssessment(RiskLevel.NEUTRAL, "Moderate"),
                context
        );

        var prompt = delegate.getPromptRunner().getLlmInvocations().get(0).getPrompt();
        assertTrue(prompt.contains("Buy 50% at $150"));
    }

    @Test
    void portfolioDecision_promptContainsRiskDebateHistory() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var delegate = fake.getDelegate();
        delegate.expectResponse(new PortfolioDecisionOutput(PortfolioRating.BUY, "Summary", "Thesis", null, null));
        delegate.expectResponse(new PortfolioDecisionOutput(PortfolioRating.BUY, "Summary", "Thesis", null, null));

        var portfolioManager = new PortfolioManager();
        portfolioManager.portfolioDecision(
                ticker(), debateState(),
                "AAPL: Strong fundamentals, buy at $150.",
                "Buy 50% at $150",
                new RiskAssessment(RiskLevel.NEUTRAL, "Moderate"),
                context
        );

        var prompt = delegate.getPromptRunner().getLlmInvocations().get(0).getPrompt();
        assertTrue(prompt.contains("strong growth"));
        assertTrue(prompt.contains("valuation concern"));
        
        
    }

    @Test
    void portfolioDecision_promptIsNonEmpty() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var delegate = fake.getDelegate();
        delegate.expectResponse(new PortfolioDecisionOutput(PortfolioRating.BUY, "Summary", "Thesis", null, null));
        delegate.expectResponse(new PortfolioDecisionOutput(PortfolioRating.BUY, "Summary", "Thesis", null, null));

        var portfolioManager = new PortfolioManager();
        portfolioManager.portfolioDecision(
                ticker(), debateState(),
                "AAPL: Strong fundamentals, buy at $150.",
                "Buy 50% at $150",
                new RiskAssessment(RiskLevel.NEUTRAL, "Moderate"),
                context
        );

        var prompt = delegate.getPromptRunner().getLlmInvocations().get(0).getPrompt();
        assertFalse(prompt.isBlank());
    }

    @Test
    void portfolioDecision_structuredOutputFirstThenStringFallback() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var delegate = fake.getDelegate();

        // First call: structured output fails (returns "null" which causes deserialization to fail)
        delegate.expectResponse("null");
        // Second call: String fallback succeeds
        delegate.expectResponse("BUY 50% of AAPL at $150. Hold for 6 months.");

        var portfolioManager = new PortfolioManager();
        portfolioManager.portfolioDecision(
                ticker(), debateState(),
                "AAPL: Strong fundamentals, buy at $150.",
                "Buy 50% at $150",
                new RiskAssessment(RiskLevel.NEUTRAL, "Moderate"),
                context
        );

        var invocations = delegate.getPromptRunner().getLlmInvocations();
        // Should have 2 invocations: structured attempt (fails) + String fallback (succeeds)
        assertEquals(2, invocations.size());
    }
}