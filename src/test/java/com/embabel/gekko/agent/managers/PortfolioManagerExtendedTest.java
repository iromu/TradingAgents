package com.embabel.gekko.agent.managers;

import com.embabel.gekko.agent.FakeActionContext;
import com.embabel.gekko.agent.RiskAssessment;
import com.embabel.gekko.agent.RiskAssessmentOutput;
import com.embabel.gekko.agent.RiskLevel;
import com.embabel.gekko.domain.PortfolioDecisionOutput;
import com.embabel.gekko.domain.PortfolioRating;
import com.embabel.gekko.domain.ResearchTypes;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Extended unit tests for PortfolioManager.
 * Covers: structured output fallback, edge cases, and prompt content.
 */
class PortfolioManagerExtendedTest {

    private static final RiskAssessment NEUTRAL_RISK = new RiskAssessment(RiskLevel.NEUTRAL, "Moderate risk");

    private static ResearchTypes.Ticker ticker() {
        return new ResearchTypes.Ticker("AAPL", "");
    }

    private static ResearchTypes.InvestmentDebateState debateState() {
        return new ResearchTypes.InvestmentDebateState(
                List.of("bull argument", "bear argument"),
                List.of("bull argument"),
                List.of("bear argument"),
                "bear argument", 2,
                new ResearchTypes.DebateBriefs("Fundamentals brief", "Market brief", "News brief", "Social brief")
        );
    }

    @Test
    void portfolioDecision_returnsRenderedMarkdown() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var delegate = fake.getDelegate();

        // Structured output succeeds: PortfolioDecisionOutput.render() produces markdown
        delegate.expectResponse(new PortfolioDecisionOutput(PortfolioRating.BUY, "50% of AAPL", "AAPL is a strong buy based on fundamentals.", null, null));

        var portfolioManager = new PortfolioManager();
        var result = portfolioManager.portfolioDecision(
                ticker(), debateState(),
                "AAPL is a strong buy based on fundamentals.",
                "Buy 50% at $150",
                new RiskAssessment(RiskLevel.NEUTRAL, "Moderate risk"),
                context
        );

        // Result should be the rendered markdown from PortfolioDecisionOutput
        assertNotNull(result);
        assertFalse(result.isBlank());
        assertTrue(result.contains("BUY"));
    }

    @Test
    void portfolioDecision_structuredOutputFallsBackToString() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var delegate = fake.getDelegate();

        // Structured output attempt fails (invalid fields), falls back to String
        delegate.expectResponse("null"); // structured attempt fails
        delegate.expectResponse("BUY 50% of AAPL at $150. Hold for 6 months."); // String fallback

        var portfolioManager = new PortfolioManager();
        var result = portfolioManager.portfolioDecision(
                ticker(), debateState(),
                "AAPL is a strong buy based on fundamentals.",
                "Buy 50% at $150",
                new RiskAssessment(RiskLevel.NEUTRAL, "Moderate risk"),
                context
        );

        // Result should be the fallback string since structured output failed
        assertNotNull(result);
        assertFalse(result.isBlank());
    }

    @Test
    void portfolioDecision_promptContainsTraderProposal() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var delegate = fake.getDelegate();
        delegate.expectResponse(new PortfolioDecisionOutput(PortfolioRating.BUY, "50% of AAPL", "AAPL is a strong buy.", null, null));

        var portfolioManager = new PortfolioManager();
        portfolioManager.portfolioDecision(
                ticker(), debateState(),
                "Research plan", "Buy 50% at $150 with stop-loss at $140",
                new RiskAssessment(RiskLevel.NEUTRAL, "Moderate"),
                context
        );

        var prompt = delegate.getPromptRunner().getLlmInvocations().get(0).getPrompt();
        assertTrue(prompt.contains("Buy 50% at $150"));
        assertTrue(prompt.contains("stop-loss"));
    }

    @Test
    void portfolioDecision_promptContainsDebateHistory() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var delegate = fake.getDelegate();
        delegate.expectResponse(new PortfolioDecisionOutput(PortfolioRating.BUY, "50% of AAPL", "AAPL is a strong buy.", null, null));

        var portfolioManager = new PortfolioManager();
        portfolioManager.portfolioDecision(
                ticker(),
                new ResearchTypes.InvestmentDebateState(
                        List.of("bull: strong growth", "bear: valuation concern"),
                        List.of("bull: strong growth"),
                        List.of("bear: valuation concern"),
                        "bear: valuation concern", 2,
                        new ResearchTypes.DebateBriefs("F", "M", "N", "S")
                ),
                "Research plan", "Trader proposal",
                new RiskAssessment(RiskLevel.NEUTRAL, "Moderate"),
                context
        );

        var prompt = delegate.getPromptRunner().getLlmInvocations().get(0).getPrompt();
        assertTrue(prompt.contains("strong growth"));
        assertTrue(prompt.contains("valuation concern"));
    }

    @Test
    void portfolioDecision_promptContainsRiskLevelCONSERVATIVE() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var delegate = fake.getDelegate();
        delegate.expectResponse(new PortfolioDecisionOutput(PortfolioRating.SELL, "100% of AAPL", "Risk is too high.", null, null));

        var portfolioManager = new PortfolioManager();
        portfolioManager.portfolioDecision(
                ticker(), debateState(),
                "Research plan", "Trader proposal",
                new RiskAssessment(RiskLevel.CONSERVATIVE, "Avoid risk"),
                context
        );

        var prompt = delegate.getPromptRunner().getLlmInvocations().get(0).getPrompt();
        assertTrue(prompt.contains("CONSERVATIVE"));
    }

    @Test
    void portfolioDecision_promptContainsRiskLevelRISKY() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var delegate = fake.getDelegate();
        delegate.expectResponse(new PortfolioDecisionOutput(PortfolioRating.BUY, "100% of AAPL", "High risk high reward.", null, null));

        var portfolioManager = new PortfolioManager();
        portfolioManager.portfolioDecision(
                ticker(), debateState(),
                "Research plan", "Trader proposal",
                new RiskAssessment(RiskLevel.RISKY, "High risk high reward"),
                context
        );

        var prompt = delegate.getPromptRunner().getLlmInvocations().get(0).getPrompt();
        assertTrue(prompt.contains("RISKY"));
    }

    @Test
    void portfolioDecision_promptIsNonEmpty() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var delegate = fake.getDelegate();
        delegate.expectResponse(new PortfolioDecisionOutput(PortfolioRating.BUY, "50% of AAPL", "AAPL is a strong buy.", null, null));

        var portfolioManager = new PortfolioManager();
        portfolioManager.portfolioDecision(
                ticker(), debateState(),
                "Research plan", "Trader proposal",
                new RiskAssessment(RiskLevel.NEUTRAL, "Moderate"),
                context
        );

        var prompt = delegate.getPromptRunner().getLlmInvocations().get(0).getPrompt();
        assertFalse(prompt.isBlank());
    }

    @Test
    void portfolioDecision_promptContainsTicker() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var delegate = fake.getDelegate();
        delegate.expectResponse(new PortfolioDecisionOutput(PortfolioRating.BUY, "50% of AAPL", "AAPL is a strong buy.", null, null));

        var portfolioManager = new PortfolioManager();
        portfolioManager.portfolioDecision(
                ticker(), debateState(),
                "Research plan", "Trader proposal",
                new RiskAssessment(RiskLevel.NEUTRAL, "Moderate"),
                context
        );

        var prompt = delegate.getPromptRunner().getLlmInvocations().get(0).getPrompt();
        assertTrue(prompt.contains("AAPL"));
    }

    @Test
    void portfolioDecision_promptContainsResearchPlan() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var delegate = fake.getDelegate();
        delegate.expectResponse(new PortfolioDecisionOutput(PortfolioRating.BUY, "50% of AAPL", "AAPL is a strong buy.", null, null));

        var portfolioManager = new PortfolioManager();
        portfolioManager.portfolioDecision(
                ticker(), debateState(),
                "AAPL: Strong fundamentals, AI growth, buy at $150.",
                "Trader proposal",
                new RiskAssessment(RiskLevel.NEUTRAL, "Moderate"),
                context
        );

        var prompt = delegate.getPromptRunner().getLlmInvocations().get(0).getPrompt();
        assertTrue(prompt.contains("Strong fundamentals"));
        assertTrue(prompt.contains("AI growth"));
    }

    @Test
    void portfolioDecision_promptContainsRiskDebateHistory() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var delegate = fake.getDelegate();
        delegate.expectResponse(new PortfolioDecisionOutput(PortfolioRating.BUY, "50% of AAPL", "AAPL is a strong buy.", null, null));

        var portfolioManager = new PortfolioManager();
        portfolioManager.portfolioDecision(
                ticker(),
                new ResearchTypes.InvestmentDebateState(
                        List.of("bull: strong revenue growth", "bear: valuation concern"),
                        List.of("bull: strong revenue growth"),
                        List.of("bear: valuation concern"),
                        "bear: valuation concern", 2,
                        new ResearchTypes.DebateBriefs("F", "M", "N", "S")
                ),
                "Research plan", "Trader proposal",
                NEUTRAL_RISK, context
        );

        var prompt = delegate.getPromptRunner().getLlmInvocations().get(0).getPrompt();
        assertTrue(prompt.contains("strong revenue growth"));
        assertTrue(prompt.contains("valuation concern"));
    }
}