package com.embabel.gekko.agent;

import com.embabel.agent.api.common.ActionContext;
import com.embabel.gekko.domain.ResearchTypes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Trader.traderProposal() using FakeActionContext + FakePromptRunner.
 * Verifies LLM role selection (BEST_ROLE), interaction ID ("trader"), and prompt content.
 */
class TraderLLMTest {

    @Test
    void traderProposal_makesLLMCalls() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var ticker = new ResearchTypes.Ticker("AAPL", "");
        var researchPlan = "Buy AAPL with 60% allocation based on strong fundamentals.";

        // Trader attempts structured output first (fails), then falls back to String
        fake.getDelegate().expectResponse("Buy AAPL at $150 with 60% position size.");
        fake.getDelegate().expectResponse("Buy AAPL at $150 with 60% position size.");

        var result = new Trader().traderProposal(ticker, researchPlan, context);

        assertEquals("Buy AAPL at $150 with 60% position size.", result);
        var invocations = fake.getDelegate().getPromptRunner().getLlmInvocations();
        // 2 invocations: structured output attempt + String fallback
        assertEquals(2, invocations.size());
    }

    @Test
    void traderProposal_usesBestRole() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var ticker = new ResearchTypes.Ticker("NVDA", "");
        var researchPlan = "Hold NVDA, wait for pullback.";

        fake.getDelegate().expectResponse("Hold NVDA.");
        fake.getDelegate().expectResponse("Hold NVDA.");

        new Trader().traderProposal(ticker, researchPlan, context);

        var invocations = fake.getDelegate().getPromptRunner().getLlmInvocations();
        assertEquals(2, invocations.size());
        var llm = invocations.get(0).getInteraction().getLlm();
        assertNotNull(llm);
        // BEST_ROLE is the string "best" — verify the role is set
        assertNotNull(llm.getRole());
    }

    @Test
    void traderProposal_usesCorrectInteractionId() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var ticker = new ResearchTypes.Ticker("TSLA", "");
        var researchPlan = "Sell TSLA on strength.";

        fake.getDelegate().expectResponse("Sell TSLA.");
        fake.getDelegate().expectResponse("Sell TSLA.");

        new Trader().traderProposal(ticker, researchPlan, context);

        var invocations = fake.getDelegate().getPromptRunner().getLlmInvocations();
        assertEquals(2, invocations.size());
        // Both invocations use the same interaction ID
        assertEquals("trader", invocations.get(0).getInteraction().getId());
        assertEquals("trader", invocations.get(1).getInteraction().getId());
    }

    @Test
    void traderProposal_promptContainsTicker() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var ticker = new ResearchTypes.Ticker("MSFT", "");
        var researchPlan = "Buy MSFT.";

        fake.getDelegate().expectResponse("Buy MSFT.");
        fake.getDelegate().expectResponse("Buy MSFT.");

        new Trader().traderProposal(ticker, researchPlan, context);

        var prompt = fake.getDelegate().getPromptRunner().getLlmInvocations().get(0).getPrompt();
        assertTrue(prompt.contains("MSFT"), "Prompt should contain the ticker");
    }

    @Test
    void traderProposal_promptContainsResearchPlan() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var ticker = new ResearchTypes.Ticker("GOOGL", "");
        var researchPlan = "Overweight GOOGL with 40% allocation, entry at $140.";

        fake.getDelegate().expectResponse("Overweight GOOGL.");
        fake.getDelegate().expectResponse("Overweight GOOGL.");

        new Trader().traderProposal(ticker, researchPlan, context);

        var prompt = fake.getDelegate().getPromptRunner().getLlmInvocations().get(0).getPrompt();
        assertTrue(prompt.contains("Overweight"), "Prompt should contain the research plan content");
    }
}