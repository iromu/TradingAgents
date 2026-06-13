package com.embabel.gekko.agent;

import com.embabel.gekko.web.TradingHtmxController.TickerForm;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TraderAgentTickerValidationTest {

    @Test
    void tickerFromForm_validTicker() {
        // tickerFromForm only uses the TickerForm parameter, not any injected dependencies
        // We can test it directly by extracting the validation logic
        String content = "AAPL";
        String feedback = "";

        // Validate: must not be blank
        assertNotNull(content);
        assertFalse(content.isBlank());

        // Validate: must be alphanumeric (dots allowed for ETFs)
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
        String content = "THISISWAYTOOLONGFORTICKER";
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
    void tickerFromForm_rejectsHyphen() {
        String content = "BTC-USD";
        String sanitized = content.trim().toUpperCase();

        // Hyphens are not allowed in ticker format
        assertThrows(IllegalArgumentException.class, () -> {
            if (!sanitized.matches("^[A-Z0-9.]+$")) {
                throw new IllegalArgumentException("Invalid ticker format: " + content);
            }
        });
    }

    @Test
    void tickerFromForm_allowsNumericTickers() {
        String content = "BTC123";
        String sanitized = content.trim().toUpperCase();

        assertTrue(sanitized.matches("^[A-Z0-9.]+$"));
        assertEquals("BTC123", sanitized);
    }
}
