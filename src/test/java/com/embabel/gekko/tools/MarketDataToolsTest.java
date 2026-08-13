package com.embabel.gekko.tools;

import com.embabel.gekko.dataflows.YFinService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketDataToolsTest {

    @Mock
    private YFinService yFinService;

    @Test
    void getStockData_returnsValidJsonOnSuccess() throws Exception {
        // Given
        MarketDataTools tools = new MarketDataTools(yFinService);
        String mockJson = "{\"open\":150.0,\"close\":151.5,\"high\":152.0,\"low\":149.5,\"volume\":1000000}";
        when(yFinService.getYFinDataOnline("AAPL", "2026-01-01", "2026-01-31")).thenReturn(mockJson);

        // When
        String result = tools.get_stock_data("AAPL", "2026-01-01", "2026-01-31");

        // Then
        assertNotNull(result);
        assertTrue(result.contains("open"));
        assertTrue(result.contains("close"));
    }

    @Test
    void getStockData_returnsErrorOnFailure() throws Exception {
        // Given
        MarketDataTools tools = new MarketDataTools(yFinService);
        when(yFinService.getYFinDataOnline("INVALID", "2026-01-01", "2026-01-31"))
                .thenThrow(new RuntimeException("Symbol not found"));

        // When
        String result = tools.get_stock_data("INVALID", "2026-01-01", "2026-01-31");

        // Then — error handling should return a descriptive error string
        assertNotNull(result);
        assertTrue(result.contains("Error fetching stock data"));
        assertTrue(result.contains("INVALID"));
    }

    @Test
    void getIndicators_returnsCalculations() throws Exception {
        // Given
        MarketDataTools tools = new MarketDataTools(yFinService);
        when(yFinService.getStockStatsIndicatorsWindowBatch("AAPL", List.of("SMA", "RSI"), "2026-01-31", 30))
                .thenReturn("SMA(20): 150.25\nRSI(14): 65.2");

        // When
        String result = tools.get_indicators("AAPL", "SMA,RSI", "2026-01-31", 30);

        // Then
        assertNotNull(result);
        assertTrue(result.contains("SMA"));
        assertTrue(result.contains("RSI"));
    }

    @Test
    void getIndicators_handlesPartialFailure() throws Exception {
        // Given
        MarketDataTools tools = new MarketDataTools(yFinService);
        // The batch method handles partial failures internally
        when(yFinService.getStockStatsIndicatorsWindowBatch("AAPL", List.of("SMA", "INVALID"), "2026-01-31", 30))
                .thenReturn("SMA(10): 150.5\nError calculating INVALID: Unknown indicator");

        // When
        String result = tools.get_indicators("AAPL", "SMA,INVALID", "2026-01-31", 30);

        // Then — should contain the successful result AND the error for the failed one
        assertNotNull(result);
        assertTrue(result.contains("SMA"));
        assertTrue(result.contains("Error calculating INVALID"));
    }

    @Test
    void getIndicators_handlesEmptyIndicatorList() throws Exception {
        // Given
        MarketDataTools tools = new MarketDataTools(yFinService);

        // When
        String result = tools.get_indicators("AAPL", "", "2026-01-31", 30);

        // Then — empty indicator list returns a descriptive message
        assertNotNull(result);
        assertTrue(result.contains("No valid indicator codes"));
    }

    @Test
    void getIndicators_handlesWhitespaceOnlyIndicator() throws Exception {
        // Given
        MarketDataTools tools = new MarketDataTools(yFinService);

        // When
        String result = tools.get_indicators("AAPL", "   ", "2026-01-31", 30);

        // Then — whitespace-only returns a descriptive message
        assertNotNull(result);
        assertTrue(result.contains("No valid indicator codes"));
    }
}
