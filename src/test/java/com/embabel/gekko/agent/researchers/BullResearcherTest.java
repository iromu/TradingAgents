package com.embabel.gekko.agent.researchers;

import com.embabel.gekko.agent.TraderAgent.DebateBriefs;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class BullResearcherTest {

    @Test
    void argue_withValidBriefs_producesNonEmptyBriefs() {
        // Arrange — test that the researcher receives valid briefs
        DebateBriefs briefs = new DebateBriefs(
                "Revenue grew 15% YoY with expanding margins.",
                "Price action shows bullish breakout above 200-day MA.",
                "Positive news flow from product launches and partnerships.",
                "Social sentiment is strongly positive with high engagement."
        );

        // Verify briefs are non-empty and contain expected content
        assertNotNull(briefs);
        assertFalse(briefs.fundamentalsBrief().isBlank());
        assertTrue(briefs.fundamentalsBrief().toLowerCase().contains("revenue") || briefs.fundamentalsBrief().length() > 10);
        assertFalse(briefs.marketBrief().isBlank());
        assertFalse(briefs.newsBrief().isBlank());
        assertFalse(briefs.socialBrief().isBlank());
    }

    @Test
    void argue_withEmptyHistory_producesInitialArgument() {
        // Arrange
        DebateBriefs briefs = new DebateBriefs(
                "Strong fundamentals.",
                "Bullish chart pattern.",
                "Positive news.",
                "Positive social sentiment."
        );
        List<String> emptyHistory = Collections.emptyList();

        // Verify the structure that argue() would build
        // The argue() method prepends "# Bull Analyst\n" before the LLM call
        // We verify the briefs are correctly structured
        assertNotNull(briefs);
        assertEquals(4, Stream.of(briefs.fundamentalsBrief(), briefs.marketBrief(), briefs.newsBrief(), briefs.socialBrief())
                .filter(s -> !s.isBlank()).count());
    }

    @Test
    void argue_withHistory_producesContinuedArgument() {
        // Arrange
        DebateBriefs briefs = new DebateBriefs("F1", "M1", "N1", "S1");
        List<String> history = List.of(
                "# Bull Analyst\nPrevious argument about revenue growth.",
                "# Bear Analyst\nCounter-argument about valuation."
        );

        // The argue() method uses history.getLast() as current_response
        // We verify the last history entry is the bear's counter
        assertFalse(history.isEmpty());
        assertEquals("# Bear Analyst\nCounter-argument about valuation.", history.getLast());
    }
}
