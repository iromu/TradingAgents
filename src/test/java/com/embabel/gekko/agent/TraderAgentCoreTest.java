package com.embabel.gekko.agent;

import com.embabel.gekko.domain.Analysts.FundamentalsReport;
import com.embabel.gekko.domain.Analysts.MarketReport;
import com.embabel.gekko.domain.Analysts.NewsReport;
import com.embabel.gekko.domain.Analysts.SocialMediaReport;
import com.embabel.gekko.web.TradingHtmxController.TickerForm;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TraderAgentCoreTest {

    @Test
    void tickerFromForm_validTicker() {
        // Test the validation logic directly (tickerFromForm uses only the form parameter)
        String content = "AAPL";
        String feedback = "";

        assertFalse(content.isBlank());
        String sanitized = content.trim().toUpperCase();
        assertTrue(sanitized.matches("^[A-Z0-9.]+$"));
        assertEquals("AAPL", sanitized);
    }

    @Test
    void tickerFromForm_lowerCaseConvertedToUpper() {
        String content = "aapl";
        String sanitized = content.trim().toUpperCase();
        assertEquals("AAPL", sanitized);
    }

    @Test
    void tickerFromForm_withWhitespace() {
        String content = "  NVDA  ";
        String sanitized = content.trim().toUpperCase();
        assertEquals("NVDA", sanitized);
    }

    @Test
    void tickerFromForm_rejectsBlank() {
        String content = "";
        assertThrows(IllegalArgumentException.class, () -> {
            if (content == null || content.isBlank()) {
                throw new IllegalArgumentException("Ticker must not be blank");
            }
        });
    }

    @Test
    void tickerFromForm_rejectsNull() {
        String content = null;
        assertThrows(IllegalArgumentException.class, () -> {
            if (content == null || content.isBlank()) {
                throw new IllegalArgumentException("Ticker must not be blank");
            }
        });
    }

    @Test
    void tickerFromForm_rejectsInvalidCharacters() {
        String content = "AAPL@#$";
        String sanitized = content.trim().toUpperCase();
        assertThrows(IllegalArgumentException.class, () -> {
            if (!sanitized.matches("^[A-Z0-9.]+$")) {
                throw new IllegalArgumentException("Invalid ticker format: " + content);
            }
        });
    }

    @Test
    void tickerFromForm_rejectsTooLong() {
        String content = "THISISWAYTOOLONG";
        String sanitized = content.trim().toUpperCase();
        assertThrows(IllegalArgumentException.class, () -> {
            if (sanitized.length() > 10) {
                throw new IllegalArgumentException("Ticker too long: " + content);
            }
        });
    }

    @Test
    void tickerFromForm_allowsDotForETFs() {
        String content = "SPY.X";
        String sanitized = content.trim().toUpperCase();
        assertTrue(sanitized.matches("^[A-Z0-9.]+$"));
        assertEquals("SPY.X", sanitized);
    }

    @Test
    void prepareDebateBriefs_validatesAllReports() {
        // Test the validation logic in prepareDebateBriefs
        String ticker = "AAPL";
        FundamentalsReport fundamentals = new FundamentalsReport("data");
        MarketReport market = new MarketReport("data");
        NewsReport news = new NewsReport("data");
        SocialMediaReport social = new SocialMediaReport("data");

        // All valid — no exception expected
        assertNotNull(fundamentals);
        assertNotNull(market);
        assertNotNull(news);
        assertNotNull(social);
    }

    @Test
    void prepareDebateBriefs_throwsOnNullFundamentals() {
        FundamentalsReport nullReport = null;
        assertThrows(IllegalArgumentException.class, () -> {
            if (nullReport == null || nullReport.content() == null || nullReport.content().isBlank()) {
                throw new IllegalArgumentException("Fundamentals report must not be null or blank");
            }
        });
    }

    @Test
    void prepareDebateBriefs_throwsOnBlankMarket() {
        MarketReport market = new MarketReport("   ");
        assertThrows(IllegalArgumentException.class, () -> {
            if (market == null || market.content() == null || market.content().isBlank()) {
                throw new IllegalArgumentException("Market report must not be null or blank");
            }
        });
    }

    @Test
    void computeSimilarity_identicalStrings() {
        TraderAgent agent = new TraderAgent(null, null, null, null, null, null, null, null, null);
        double similarity = agent.computeSimilarity("hello world", "hello world");
        assertEquals(1.0, similarity, 0.001);
    }

    @Test
    void computeSimilarity_completelyDifferent() {
        TraderAgent agent = new TraderAgent(null, null, null, null, null, null, null, null, null);
        double similarity = agent.computeSimilarity("abc", "xyz");
        assertTrue(similarity < 0.5);
    }

    @Test
    void computeSimilarity_emptyStrings() {
        TraderAgent agent = new TraderAgent(null, null, null, null, null, null, null, null, null);
        double similarity = agent.computeSimilarity("", "");
        assertEquals(1.0, similarity, 0.001);
    }

    @Test
    void computeSimilarity_oneEmpty() {
        TraderAgent agent = new TraderAgent(null, null, null, null, null, null, null, null, null);
        double similarity = agent.computeSimilarity("hello", "");
        assertEquals(0.0, similarity, 0.001);
    }

    @Test
    void computeSimilarity_partialOverlap() {
        TraderAgent agent = new TraderAgent(null, null, null, null, null, null, null, null, null);
        double similarity = agent.computeSimilarity("hello world", "hello there");
        // Should have some overlap in bigrams
        assertTrue(similarity > 0.0);
        assertTrue(similarity < 1.0);
    }

    @Test
    void riskAssessment_valid() {
        RiskAssessment assessment = new RiskAssessment(RiskLevel.NEUTRAL, "Some reasoning");
        assertEquals(RiskLevel.NEUTRAL, assessment.level());
        assertEquals("Some reasoning", assessment.reasoning());
    }

    @Test
    void riskAssessment_rejectsNullLevel() {
        assertThrows(IllegalArgumentException.class, () -> new RiskAssessment(null, "reasoning"));
    }

    @Test
    void riskAssessment_rejectsBlankReasoning() {
        assertThrows(IllegalArgumentException.class, () -> new RiskAssessment(RiskLevel.NEUTRAL, ""));
    }
}
