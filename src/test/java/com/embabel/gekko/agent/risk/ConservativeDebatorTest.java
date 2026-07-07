package com.embabel.gekko.agent.risk;

import com.embabel.gekko.agent.FakeActionContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ConservativeDebator using FakePromptRunner.
 * Verifies BEST_ROLE, interaction ID, and prompt content.
 */
class ConservativeDebatorTest {

    @Test
    void argue_usesBEST_ROLE() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var delegate = fake.getDelegate();
        delegate.expectResponse("Conservative argument: preserve capital.");
        var debator = new ConservativeDebator();
        var result = debator.argue(
                "BUY", "Market is hot.", "Social sentiment positive.",
                "Positive news.", "Strong fundamentals.",
                "", "", "", context
        );
        var invocations = delegate.getPromptRunner().getLlmInvocations();
        assertEquals(1, invocations.size());
        var invocation = invocations.get(0);
        assertEquals("conservativeDebator", invocation.getInteraction().getId());
        assertEquals("best", invocation.getInteraction().getLlm().getRole());
        assertEquals("Conservative argument: preserve capital.", result);
    }

    @Test
    void argue_promptContainsTraderDecision() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var delegate = fake.getDelegate();
        delegate.expectResponse("Conservative response.");
        var debator = new ConservativeDebator();
        debator.argue(
                "SELL", "Market is hot.", "Social sentiment positive.",
                "Positive news.", "Strong fundamentals.",
                "", "", "", context
        );
        var prompt = delegate.getPromptRunner().getLlmInvocations().get(0).getPrompt();
        assertTrue(prompt.contains("SELL"));
    }

    @Test
    void argue_promptContainsFundamentals() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var delegate = fake.getDelegate();
        delegate.expectResponse("Conservative response.");
        var debator = new ConservativeDebator();
        debator.argue(
                "BUY", "Market is hot.", "Social sentiment positive.",
                "Positive news.", "Revenue declined 10%.",
                "", "", "", context
        );
        var prompt = delegate.getPromptRunner().getLlmInvocations().get(0).getPrompt();
        assertTrue(prompt.contains("Revenue declined 10%"));
    }
}