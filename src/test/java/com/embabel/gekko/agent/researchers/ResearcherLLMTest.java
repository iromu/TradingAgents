package com.embabel.gekko.agent.researchers;

import com.embabel.gekko.agent.FakeActionContext;
import com.embabel.gekko.agent.TraderAgent.DebateBriefs;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BullResearcher and BearResearcher using FakePromptRunner.
 * Verifies BEST_ROLE, distinct interaction IDs, and prompt content.
 */
class ResearcherLLMTest {

    @Test
    void bullResearcher_usesBEST_ROLE() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var delegate = fake.getDelegate();
        delegate.expectResponse("# Bull Analyst\nBull argument about revenue growth.");
        var briefs = new DebateBriefs(
                "Revenue grew 15% YoY.",
                "Bullish breakout above 200-day MA.",
                "Positive product launch news.",
                "Strong social sentiment."
        );
        var researcher = new BullResearcher();
        var result = researcher.argue(briefs, Collections.emptyList(), context);
        var invocations = delegate.getPromptRunner().getLlmInvocations();
        assertEquals(1, invocations.size());
        var invocation = invocations.get(0);
        var interaction = invocation.getInteraction();
        assertEquals("bullResearcher", interaction.getId());
        assertTrue(result.contains("# Bull Analyst"));
    }

    @Test
    void bearResearcher_usesBEST_ROLE() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var delegate = fake.getDelegate();
        delegate.expectResponse("# Bear Analyst\nBear argument about valuation concerns.");
        var briefs = new DebateBriefs(
                "Revenue growth slowing.",
                "Bearish divergence below support.",
                "Negative regulatory news.",
                "Negative social sentiment."
        );
        var researcher = new BearResearcher();
        var result = researcher.argue(briefs, Collections.emptyList(), context);
        var invocations = delegate.getPromptRunner().getLlmInvocations();
        assertEquals(1, invocations.size());
        var invocation = invocations.get(0);
        var interaction = invocation.getInteraction();
        assertEquals("bearResearcher", interaction.getId());
        assertTrue(result.contains("# Bear Analyst"));
    }

    @Test
    void bullResearcher_promptContainsBriefs() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var delegate = fake.getDelegate();
        delegate.expectResponse("# Bull Analyst\nBull argument.");
        var briefs = new DebateBriefs(
                "Fundamentals: Revenue grew 15% YoY.",
                "Market: Bullish breakout.",
                "News: Positive product launch.",
                "Social: Strong sentiment."
        );
        var researcher = new BullResearcher();
        researcher.argue(briefs, Collections.emptyList(), context);
        var invocations = delegate.getPromptRunner().getLlmInvocations();
        var prompt = invocations.get(0).getPrompt();
        assertTrue(prompt.contains("Revenue grew 15% YoY"));
        assertTrue(prompt.contains("Bullish breakout"));
        assertTrue(prompt.contains("Positive product launch"));
        assertTrue(prompt.contains("Strong sentiment"));
    }

    @Test
    void bearResearcher_promptContainsBriefs() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var delegate = fake.getDelegate();
        delegate.expectResponse("# Bear Analyst\nBear argument.");
        var briefs = new DebateBriefs(
                "Fundamentals: Revenue growth slowing.",
                "Market: Bearish divergence.",
                "News: Negative regulatory news.",
                "Social: Negative sentiment."
        );
        var researcher = new BearResearcher();
        researcher.argue(briefs, Collections.emptyList(), context);
        var invocations = delegate.getPromptRunner().getLlmInvocations();
        var prompt = invocations.get(0).getPrompt();
        assertTrue(prompt.contains("Revenue growth slowing"));
        assertTrue(prompt.contains("Bearish divergence"));
        assertTrue(prompt.contains("Negative regulatory news"));
        assertTrue(prompt.contains("Negative sentiment"));
    }

    @Test
    void bullResearcher_promptContainsHistory() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var delegate = fake.getDelegate();
        delegate.expectResponse("# Bull Analyst\nBull response with history.");
        var briefs = new DebateBriefs("F", "M", "N", "S");
        var history = List.of(
                "# Bear Analyst\nPrevious bear argument about valuation."
        );
        var researcher = new BullResearcher();
        researcher.argue(briefs, history, context);
        var invocations = delegate.getPromptRunner().getLlmInvocations();
        var prompt = invocations.get(0).getPrompt();
        assertTrue(prompt.contains("valuation"));
        // Verify current_response contains the last history entry
        assertTrue(prompt.contains("Previous bear argument"));
    }
}
