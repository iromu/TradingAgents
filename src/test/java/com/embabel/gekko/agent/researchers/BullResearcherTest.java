package com.embabel.gekko.agent.researchers;

import com.embabel.gekko.agent.FakeActionContext;
import com.embabel.gekko.domain.ResearchTypes.DebateBriefs;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BullResearcher.
 * Tests both pure logic (DebateBriefs structure) and LLM interaction patterns (FakePromptRunner).
 */
class BullResearcherTest {

    @Test
    void argue_withValidBriefs_producesNonEmptyBriefs() {
        DebateBriefs briefs = new DebateBriefs(
                "Revenue grew 15% YoY with expanding margins.",
                "Price action shows bullish breakout above 200-day MA.",
                "Positive news flow from product launches and partnerships.",
                "Social sentiment is strongly positive with high engagement."
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
                "Strong fundamentals.",
                "Bullish chart pattern.",
                "Positive news.",
                "Positive social sentiment."
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
                "# Bull Analyst\nPrevious argument about revenue growth.",
                "# Bear Analyst\nCounter-argument about valuation."
        );
        assertFalse(history.isEmpty());
        assertEquals("# Bear Analyst\nCounter-argument about valuation.", history.getLast());
    }

    // --- argue() LLM interaction tests ---

    @Test
    void argue_usesCorrectTemplate() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var delegate = fake.getDelegate();
        delegate.expectResponse("# Bull Analyst\nBull argument.");
        var briefs = new DebateBriefs("F", "M", "N", "S");
        var researcher = new BullResearcher();
        var result = researcher.argue(briefs, Collections.emptyList(), context);
        var invocations = delegate.getPromptRunner().getLlmInvocations();
        assertEquals(1, invocations.size());
        // Verify the interaction ID is bullResearcher
        assertEquals("bullResearcher", invocations.get(0).getInteraction().getId());
    }

    @Test
    void argue_usesBEST_ROLE() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var delegate = fake.getDelegate();
        delegate.expectResponse("# Bull Analyst\nBull argument.");
        var briefs = new DebateBriefs("F", "M", "N", "S");
        var researcher = new BullResearcher();
        researcher.argue(briefs, Collections.emptyList(), context);
        var invocations = delegate.getPromptRunner().getLlmInvocations();
        var llm = invocations.get(0).getInteraction().getLlm();
        assertNotNull(llm);
        assertEquals("best", llm.getRole());
    }

    @Test
    void argue_includesHistoryInPrompt() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var delegate = fake.getDelegate();
        delegate.expectResponse("# Bull Analyst\nBull response.");
        var briefs = new DebateBriefs("F", "M", "N", "S");
        var history = List.of("# Bear Analyst\nPrevious bear argument.");
        var researcher = new BullResearcher();
        researcher.argue(briefs, history, context);
        var prompt = delegate.getPromptRunner().getLlmInvocations().get(0).getPrompt();
        assertTrue(prompt.contains("Previous bear argument"));
    }
}
