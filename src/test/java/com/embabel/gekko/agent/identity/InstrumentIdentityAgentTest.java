package com.embabel.gekko.agent.identity;

import com.embabel.gekko.dataflows.YFinService;
import com.embabel.gekko.domain.ResearchTypes;
import com.embabel.gekko.util.FileCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for InstrumentIdentityAgent.
 */
class InstrumentIdentityAgentTest {

    @TempDir
    Path tempDir;

    private FileCache cache;
    private YFinService yFinService;
    private InstrumentIdentityAgent agent;

    @BeforeEach
    void setUp() {
        cache = new FileCache();
        // Override baseDir to use temp directory
        try {
            var field = FileCache.class.getDeclaredField("baseDir");
            field.setAccessible(true);
            field.set(cache, tempDir);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set cache baseDir", e);
        }
        yFinService = mock(YFinService.class);
        agent = new InstrumentIdentityAgent(yFinService, cache);
    }

    @Test
    void resolveIdentity_returnsNullForInvalidTicker() {
        var ticker = new ResearchTypes.Ticker("", "");
        var result = agent.resolveIdentity(ticker);
        assertNull(result);
    }

    @Test
    void resolveIdentity_returnsNullForNullTicker() {
        var result = agent.resolveIdentity(null);
        assertNull(result);
    }

    @Test
    void resolveIdentity_returnsNullWhenYFinFails() throws Exception {
        var ticker = new ResearchTypes.Ticker("INVALID", "");
        when(yFinService.getTickerInfo("INVALID")).thenThrow(new RuntimeException("Yahoo Finance unavailable"));

        var result = agent.resolveIdentity(ticker);
        assertNull(result);
    }

    @Test
    void resolveIdentity_returnsNullWhenStockInvalid() throws Exception {
        var ticker = new ResearchTypes.Ticker("INVALID", "");
        yahoofinance.Stock invalidStock = mock(yahoofinance.Stock.class);
        when(invalidStock.isValid()).thenReturn(false);
        when(yFinService.getTickerInfo("INVALID")).thenReturn(invalidStock);

        var result = agent.resolveIdentity(ticker);
        assertNull(result);
    }

    @Test
    void resolveIdentity_validTickerFormat() throws Exception {
        assertTrue(agent.validateTicker(new ResearchTypes.Ticker("AAPL", "")));
        assertTrue(agent.validateTicker(new ResearchTypes.Ticker("BTC-USD", "")));
        assertTrue(agent.validateTicker(new ResearchTypes.Ticker("MSFT", "")));
    }

    @Test
    void resolveIdentity_invalidTickerFormat() throws Exception {
        assertFalse(agent.validateTicker(new ResearchTypes.Ticker("", "")));
        assertFalse(agent.validateTicker(new ResearchTypes.Ticker(null, "")));
        assertFalse(agent.validateTicker(new ResearchTypes.Ticker("a b c", "")));
    }

    @Test
    void resolveIdentity_cachesResult() throws Exception {
        // First call should compute
        var ticker = new ResearchTypes.Ticker("AAPL", "");
        yahoofinance.Stock stock = mock(yahoofinance.Stock.class);
        when(stock.isValid()).thenReturn(true);
        when(stock.getName()).thenReturn("Apple Inc.");
        when(stock.getStockExchange()).thenReturn("NASDAQ");
        when(stock.getCurrency()).thenReturn("USD");
        when(yFinService.getTickerInfo("AAPL")).thenReturn(stock);

        var result1 = agent.resolveIdentity(ticker);
        assertNotNull(result1);

        // Second call should use cache (no additional YFinService call)
        var result2 = agent.resolveIdentity(ticker);
        assertNotNull(result2);

        // Verify only one YFinService call
        verify(yFinService, times(1)).getTickerInfo("AAPL");
    }

    @Test
    void resolveIdentity_returnsInstrumentContextWithCorrectFields() throws Exception {
        var ticker = new ResearchTypes.Ticker("AAPL", "");
        yahoofinance.Stock stock = mock(yahoofinance.Stock.class);
        when(stock.isValid()).thenReturn(true);
        when(stock.getName()).thenReturn("Apple Inc.");
        when(stock.getStockExchange()).thenReturn("NASDAQ");
        when(stock.getCurrency()).thenReturn("USD");
        when(yFinService.getTickerInfo("AAPL")).thenReturn(stock);

        var result = agent.resolveIdentity(ticker);

        assertNotNull(result);
        assertEquals("AAPL", result.ticker());
        assertEquals("Apple Inc.", result.companyName());
        assertEquals("Unknown", result.sector());
        assertEquals("Unknown", result.industry());
        assertEquals("NASDAQ", result.exchange());
        assertEquals("USD", result.currency());
    }
}
