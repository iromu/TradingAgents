package com.embabel.gekko.agent;

import com.embabel.gekko.agent.TraderAgent.DebateBriefs;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DebateBriefsTest {

    @Test
    void debateBriefs_contentIncludesAllSections() {
        DebateBriefs briefs = new DebateBriefs(
                "Fundamentals: strong revenue growth",
                "Market: uptrend confirmed",
                "News: positive earnings report",
                "Social: bullish sentiment"
        );

        String content = briefs.content();
        assertTrue(content.contains("FUNDAMENTALS BRIEF"));
        assertTrue(content.contains("MARKET BRIEF"));
        assertTrue(content.contains("NEWS BRIEF"));
        assertTrue(content.contains("SOCIAL BRIEF"));
    }

    @Test
    void debateBriefs_nonEmptyBriefs() {
        DebateBriefs briefs = new DebateBriefs("a", "b", "c", "d");
        assertEquals("a", briefs.fundamentalsBrief());
        assertEquals("b", briefs.marketBrief());
        assertEquals("c", briefs.newsBrief());
        assertEquals("d", briefs.socialBrief());
    }
}
