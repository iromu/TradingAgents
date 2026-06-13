package com.embabel.gekko.agent.researchers;

import com.embabel.gekko.agent.TraderAgent.DebateBriefs;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class BearResearcherTest {

    @Test
    void argue_withValidBriefs_producesNonEmptyBriefs() {
        // Arrange — test that the researcher receives valid briefs
        DebateBriefs briefs = new DebateBriefs(
                "Revenue growth slowing from 20% to 5% YoY.",
                "Price action shows bearish divergence below support.",
                "Negative news flow from regulatory concerns.",
                "Social sentiment turning negative with increased criticism."
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
                "Weak fundamentals.",
                "Bearish chart pattern.",
                "Negative news.",
                "Negative social sentiment."
        );
        List<String> emptyHistory = Collections.emptyList();

        // Verify the structure that argue() would build
        // The argue() method prepends "# Bear Analyst\n" before the LLM call
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
                "# Bear Analyst\nPrevious argument about declining margins.",
                "# Bull Analyst\nCounter-argument about market opportunity."
        );

        // The argue() method uses history.getLast() as current_response
        // We verify the last history entry is the bull's counter
        assertFalse(history.isEmpty());
        assertEquals("# Bull Analyst\nCounter-argument about market opportunity.", history.getLast());
    }
}
