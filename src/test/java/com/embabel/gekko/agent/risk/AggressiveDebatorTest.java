package com.embabel.gekko.agent.risk;

import com.embabel.gekko.agent.FakeActionContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AggressiveDebator using FakePromptRunner.
 * Verifies BEST_ROLE, interaction ID, and prompt content.
 */
class AggressiveDebatorTest {

    @Test
    void argue_usesBEST_ROLE() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var delegate = fake.getDelegate();
        delegate.expectResponse("Aggressive argument: invest heavily in growth.");
        var debator = new AggressiveDebator();
        var result = debator.argue(
                "BUY", "Market is hot.", "Social sentiment positive.",
                "Positive news.", "Strong fundamentals.",
                "", "", "", context
        );
        var invocations = delegate.getPromptRunner().getLlmInvocations();
        assertEquals(1, invocations.size());
        var invocation = invocations.get(0);
        assertEquals("aggressiveDebator", invocation.getInteraction().getId());
        assertEquals("best", invocation.getInteraction().getLlm().getRole());
        assertEquals("Aggressive argument: invest heavily in growth.", result);
    }

    @Test
    void argue_promptContainsTraderDecision() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var delegate = fake.getDelegate();
        delegate.expectResponse("Aggressive response.");
        var debator = new AggressiveDebator();
        debator.argue(
                "BUY", "Market is hot.", "Social sentiment positive.",
                "Positive news.", "Strong fundamentals.",
                "", "", "", context
        );
        var prompt = delegate.getPromptRunner().getLlmInvocations().get(0).getPrompt();
        assertTrue(prompt.contains("BUY"));
    }

    @Test
    void argue_promptContainsFundamentals() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var delegate = fake.getDelegate();
        delegate.expectResponse("Aggressive response.");
        var debator = new AggressiveDebator();
        debator.argue(
                "BUY", "Market is hot.", "Social sentiment positive.",
                "Positive news.", "Revenue grew 20%.",
                "", "", "", context
        );
        var prompt = delegate.getPromptRunner().getLlmInvocations().get(0).getPrompt();
        assertTrue(prompt.contains("Revenue grew 20%"));
    }
}