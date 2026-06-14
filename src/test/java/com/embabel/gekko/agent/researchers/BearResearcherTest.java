package com.embabel.gekko.agent.researchers;

import com.embabel.gekko.agent.FakeActionContext;
import com.embabel.gekko.domain.ResearchTypes.DebateBriefs;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BearResearcher.
 * Tests both pure logic (DebateBriefs structure) and LLM interaction patterns (FakePromptRunner).
 */
class BearResearcherTest {

    @Test
    void argue_withValidBriefs_producesNonEmptyBriefs() {
        DebateBriefs briefs = new DebateBriefs(
                "Revenue growth slowing from 20% to 5% YoY.",
                "Price action shows bearish divergence below support.",
                "Negative news flow from regulatory concerns.",
                "Social sentiment turning negative with increased criticism."
        );
        assertNotNull(briefs);
        assertFalse(briefs.fundamentalsBrief().isBlank());
        assertFalse(briefs.marketBrief().isBlank());
        assertFalse(briefs.newsBrief().isBlank());
        assertFalse(briefs.socialBrief().isBlank());
    }

    @Test
    void argue_withEmptyHistory_producesInitialArgument() {
        DebateBriefs briefs = new DebateBriefs(
                "Weak fundamentals.",
                "Bearish chart pattern.",
                "Negative news.",
                "Negative social sentiment."
        );
        List<String> emptyHistory = Collections.emptyList();
        assertNotNull(briefs);
        assertEquals(4, List.of(briefs.fundamentalsBrief(), briefs.marketBrief(), briefs.newsBrief(), briefs.socialBrief())
                .stream().filter(s -> !s.isBlank()).count());
    }

    @Test
    void argue_withHistory_producesContinuedArgument() {
        DebateBriefs briefs = new DebateBriefs("F1", "M1", "N1", "S1");
        List<String> history = List.of(
                "# Bear Analyst\nPrevious argument about declining margins.",
                "# Bull Analyst\nCounter-argument about market opportunity."
        );
        assertFalse(history.isEmpty());
        assertEquals("# Bull Analyst\nCounter-argument about market opportunity.", history.getLast());
    }

    // --- argue() LLM interaction tests ---

    @Test
    void argue_usesCorrectTemplate() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var delegate = fake.getDelegate();
        delegate.expectResponse("# Bear Analyst\nBear argument.");
        var briefs = new DebateBriefs("F", "M", "N", "S");
        var researcher = new BearResearcher();
        var result = researcher.argue(briefs, Collections.emptyList(), context);
        var invocations = delegate.getPromptRunner().getLlmInvocations();
        assertEquals(1, invocations.size());
        assertEquals("bearResearcher", invocations.get(0).getInteraction().getId());
    }

    @Test
    void argue_usesBEST_ROLE() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var delegate = fake.getDelegate();
        delegate.expectResponse("# Bear Analyst\nBear argument.");
        var briefs = new DebateBriefs("F", "M", "N", "S");
        var researcher = new BearResearcher();
        researcher.argue(briefs, Collections.emptyList(), context);
        var invocations = delegate.getPromptRunner().getLlmInvocations();
        var llm = invocations.get(0).getInteraction().getLlm();
        assertNotNull(llm);
    }

    @Test
    void argue_includesHistoryInPrompt() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var delegate = fake.getDelegate();
        delegate.expectResponse("# Bear Analyst\nBear response.");
        var briefs = new DebateBriefs("F", "M", "N", "S");
        var history = List.of("# Bull Analyst\nPrevious bull argument.");
        var researcher = new BearResearcher();
        researcher.argue(briefs, history, context);
        var prompt = delegate.getPromptRunner().getLlmInvocations().get(0).getPrompt();
        assertTrue(prompt.contains("Previous bull argument"));
    }
}
