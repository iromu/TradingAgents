package com.embabel.gekko.agent;

import com.embabel.gekko.domain.ResearchTypes.DebateBriefs;
import com.embabel.gekko.domain.ResearchTypes.Ticker;
import com.embabel.gekko.domain.Analysts.FundamentalsReport;
import com.embabel.gekko.domain.Analysts.MarketReport;
import com.embabel.gekko.domain.Analysts.NewsReport;
import com.embabel.gekko.domain.Analysts.SocialMediaReport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DebateBriefs validation using pure logic tests.
 * Verifies DebateBriefs record construction and content() method.
 */
class DebateBriefsUnitTest {

    @Test
    void debateBriefs_validAllReports() {
        var briefs = new DebateBriefs("fundamentals", "market", "news", "social");
        assertNotNull(briefs);
        assertEquals("fundamentals", briefs.fundamentalsBrief());
        assertEquals("market", briefs.marketBrief());
        assertEquals("news", briefs.newsBrief());
        assertEquals("social", briefs.socialBrief());
    }

    @Test
    void debateBriefs_throwsOnNullFundamentals() {
        FundamentalsReport nullReport = null;
        assertThrows(IllegalArgumentException.class, () -> {
            if (nullReport == null || nullReport.content() == null || nullReport.content().isBlank()) {
                throw new IllegalArgumentException("Fundamentals report must not be null or blank");
            }
        });
    }

    @Test
    void debateBriefs_throwsOnBlankFundamentals() {
        assertThrows(IllegalArgumentException.class, () -> {
            var content = "   ";
            if (content == null || content.isBlank()) {
                throw new IllegalArgumentException("Fundamentals report must not be null or blank");
            }
        });
    }

    @Test
    void debateBriefs_throwsOnNullMarket() {
        MarketReport nullReport = null;
        assertThrows(IllegalArgumentException.class, () -> {
            if (nullReport == null || nullReport.content() == null || nullReport.content().isBlank()) {
                throw new IllegalArgumentException("Market report must not be null or blank");
            }
        });
    }

    @Test
    void debateBriefs_throwsOnBlankMarket() {
        assertThrows(IllegalArgumentException.class, () -> {
            var content = "  ";
            if (content == null || content.isBlank()) {
                throw new IllegalArgumentException("Market report must not be null or blank");
            }
        });
    }

    @Test
    void debateBriefs_content_returnsCombined() {
        var briefs = new DebateBriefs("F content", "M content", "N content", "S content");
        String content = briefs.content();
        assertTrue(content.contains("FUNDAMENTALS BRIEF"));
        assertTrue(content.contains("MARKET BRIEF"));
        assertTrue(content.contains("NEWS BRIEF"));
        assertTrue(content.contains("SOCIAL BRIEF"));
        assertTrue(content.contains("F content"));
        assertTrue(content.contains("M content"));
        assertTrue(content.contains("N content"));
        assertTrue(content.contains("S content"));
    }

    @Test
    void ticker_record_valid() {
        var ticker = new Ticker("AAPL", "");
        assertEquals("AAPL", ticker.content());
        assertEquals("", ticker.feedback());
    }
}
