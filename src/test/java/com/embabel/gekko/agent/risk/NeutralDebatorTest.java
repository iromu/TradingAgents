package com.embabel.gekko.agent.risk;

import com.embabel.gekko.agent.FakeActionContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for NeutralDebator using FakePromptRunner.
 * Verifies BEST_ROLE, interaction ID, and prompt content.
 */
class NeutralDebatorTest {

    @Test
    void argue_usesBEST_ROLE() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var delegate = fake.getDelegate();
        delegate.expectResponse("Neutral argument: balanced position.");
        var debator = new NeutralDebator();
        var result = debator.argue(
                "HOLD", "Market is hot.", "Social sentiment positive.",
                "Positive news.", "Strong fundamentals.",
                "", "", "", context
        );
        var invocations = delegate.getPromptRunner().getLlmInvocations();
        assertEquals(1, invocations.size());
        var invocation = invocations.get(0);
        assertEquals("neutralDebator", invocation.getInteraction().getId());
        assertEquals("best", invocation.getInteraction().getLlm().getRole());
        assertEquals("Neutral argument: balanced position.", result);
    }

    @Test
    void argue_promptContainsTraderDecision() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var delegate = fake.getDelegate();
        delegate.expectResponse("Neutral response.");
        var debator = new NeutralDebator();
        debator.argue(
                "HOLD", "Market is hot.", "Social sentiment positive.",
                "Positive news.", "Strong fundamentals.",
                "", "", "", context
        );
        var prompt = delegate.getPromptRunner().getLlmInvocations().get(0).getPrompt();
        assertTrue(prompt.contains("HOLD"));
    }

    @Test
    void argue_promptContainsMarketResearch() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var delegate = fake.getDelegate();
        delegate.expectResponse("Neutral response.");
        var debator = new NeutralDebator();
        debator.argue(
                "HOLD", "Market showing mixed signals.", "Social sentiment positive.",
                "Positive news.", "Strong fundamentals.",
                "", "", "", context
        );
        var prompt = delegate.getPromptRunner().getLlmInvocations().get(0).getPrompt();
        assertTrue(prompt.contains("Market showing mixed signals"));
    }
}