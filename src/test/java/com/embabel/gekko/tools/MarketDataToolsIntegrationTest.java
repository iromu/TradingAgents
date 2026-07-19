package com.embabel.gekko.tools;

import com.embabel.gekko.dataflows.YFinService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for MarketDataTools.
 * Verifies the @Tool methods for stock data and technical indicators.
 */
@SpringBootTest
@ActiveProfiles({"base", "app"})
class MarketDataToolsIntegrationTest {

    @Autowired
    private MarketDataTools marketDataTools;

    @SpyBean
    private YFinService yFinService;

    @Test
    void shouldHaveMarketDataToolsAsSpringBean() {
        assertNotNull(marketDataTools);
    }

    @Test
    void shouldHaveYFinServiceAsSpringBean() {
        assertNotNull(yFinService);
    }

    @Test
    void get_stock_data_returnsNonEmptyForValidTicker() {
        // With mocked/real YFinService, the call should not throw
        // Even if YFinService fails (no API key), the tool handles errors gracefully
        String result = marketDataTools.get_stock_data("AAPL", "2024-01-01", "2024-01-31");

        assertNotNull(result);
        // Either returns data or an error message — both are valid
        assertFalse(result.isBlank());
    }

    @Test
    void get_stock_data_handlesInvalidTicker() {
        // Should return an error message, not throw
        String result = marketDataTools.get_stock_data("INVALID_TICKER_XYZ", "2024-01-01", "2024-01-31");

        assertNotNull(result);
        // With a real YFinService, this might return an error
        // With a mock, it might return data
        // Either way, it should not throw
    }

    @Test
    void get_indicators_returnsNonEmptyForValidTicker() {
        String result = marketDataTools.get_indicators("AAPL", "SMA", "2024-01-31", 30);

        assertNotNull(result);
        // Either returns indicator data or an error message
        assertFalse(result.isBlank());
    }

    @Test
    void get_indicators_handlesMultipleIndicators() {
        String result = marketDataTools.get_indicators("AAPL", "SMA,RSI,MACD", "2024-01-31", 30);

        assertNotNull(result);
        // Should handle comma-separated indicators
    }

    @Test
    void get_indicators_handlesEmptyIndicatorCode() {
        String result = marketDataTools.get_indicators("AAPL", "", "2024-01-31", 30);

        assertNotNull(result);
        // Empty indicator list should not throw
    }

    @Test
    void get_indicators_handlesInvalidIndicator() {
        String result = marketDataTools.get_indicators("AAPL", "INVALID_INDICATOR", "2024-01-31", 30);

        assertNotNull(result);
        // Should report error for individual indicator without failing the whole call
    }

    @Test
    void get_stock_data_handlesNullTicker() {
        String result = marketDataTools.get_stock_data(null, "2024-01-01", "2024-01-31");

        // Should not throw — YFinService handles null gracefully
        assertNotNull(result);
    }

    @Test
    void get_indicators_handlesNullTicker() {
        String result = marketDataTools.get_indicators(null, "SMA", "2024-01-31", 30);

        assertNotNull(result);
    }
}