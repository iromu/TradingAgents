package com.embabel.gekko.agent.identity;

import com.embabel.gekko.dataflows.AlphaVantageService;
import com.embabel.gekko.dataflows.YFinService;
import com.embabel.gekko.domain.ResearchTypes;
import com.embabel.gekko.util.FileCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.ObjectProvider;

import java.nio.file.Path;
import java.util.Map;

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

    private static ObjectProvider<AlphaVantageService> noAvProvider() {
        ObjectProvider<AlphaVantageService> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        return provider;
    }

    private static ObjectProvider<AlphaVantageService> avProvider(AlphaVantageService service) {
        ObjectProvider<AlphaVantageService> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(service);
        return provider;
    }

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
        agent = new InstrumentIdentityAgent(yFinService, cache, noAvProvider());
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
    void resolveIdentity_cachesResult() throws Exception {
        var ticker = new ResearchTypes.Ticker("AAPL", "");
        yahoofinance.Stock stock = mock(yahoofinance.Stock.class);
        when(stock.isValid()).thenReturn(true);
        when(stock.getName()).thenReturn("Apple Inc.");
        when(stock.getStockExchange()).thenReturn("NASDAQ");
        when(stock.getCurrency()).thenReturn("USD");
        when(yFinService.getTickerInfo("AAPL")).thenReturn(stock);

        var result1 = agent.resolveIdentity(ticker);
        assertNotNull(result1);

        var result2 = agent.resolveIdentity(ticker);
        assertNotNull(result2);

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

    @Test
    void resolveIdentity_retriesOnFirstFailureThenSucceeds() throws Exception {
        var ticker = new ResearchTypes.Ticker("AAPL", "");
        yahoofinance.Stock stock = mock(yahoofinance.Stock.class);
        when(stock.isValid()).thenReturn(true);
        when(stock.getName()).thenReturn("Apple Inc.");
        when(stock.getStockExchange()).thenReturn("NASDAQ");
        when(stock.getCurrency()).thenReturn("USD");

        when(yFinService.getTickerInfo("AAPL"))
                .thenThrow(new RuntimeException("Rate limited"))
                .thenReturn(stock);

        var result = agent.resolveIdentity(ticker);
        assertNotNull(result);
        assertEquals("Apple Inc.", result.companyName());

        verify(yFinService, times(2)).getTickerInfo("AAPL");
    }

    @Test
    void resolveIdentity_failsAfterAllRetriesExhausted() throws Exception {
        var ticker = new ResearchTypes.Ticker("AAPL", "");

        when(yFinService.getTickerInfo("AAPL"))
                .thenThrow(new RuntimeException("Rate limited"))
                .thenThrow(new RuntimeException("Rate limited"))
                .thenThrow(new RuntimeException("Rate limited"));

        var result = agent.resolveIdentity(ticker);
        assertNull(result);

        verify(yFinService, times(3)).getTickerInfo("AAPL");
    }

    @Test
    void resolveIdentity_fallsBackToAlphaVantageWhenYFinFails() throws Exception {
        var ticker = new ResearchTypes.Ticker("AAPL", "");
        AlphaVantageService avService = mock(AlphaVantageService.class);
        when(avService.getOverview("AAPL")).thenReturn(Map.of(
                "Name", "Apple Inc.",
                "Sector", "Technology",
                "Industry", "Consumer Electronics",
                "Exchange", "NASDAQ",
                "Currency", "USD"
        ));

        when(yFinService.getTickerInfo("AAPL")).thenThrow(new RuntimeException("429 Too Many Requests"));

        agent = new InstrumentIdentityAgent(yFinService, cache, avProvider(avService));
        var result = agent.resolveIdentity(ticker);

        assertNotNull(result);
        assertEquals("Apple Inc.", result.companyName());
        assertEquals("Technology", result.sector());
        assertEquals("Consumer Electronics", result.industry());
        assertEquals("NASDAQ", result.exchange());
        assertEquals("USD", result.currency());
    }

    @Test
    void resolveIdentity_fallsBackToAlphaVantageWhenStockInvalid() throws Exception {
        var ticker = new ResearchTypes.Ticker("INVALID", "");
        AlphaVantageService avService = mock(AlphaVantageService.class);
        when(avService.getOverview("INVALID")).thenReturn(Map.of(
                "Name", "Invalid Company",
                "Sector", "Unknown",
                "Industry", "Unknown",
                "Exchange", "NYSE",
                "Currency", "USD"
        ));

        yahoofinance.Stock invalidStock = mock(yahoofinance.Stock.class);
        when(invalidStock.isValid()).thenReturn(false);
        when(yFinService.getTickerInfo("INVALID")).thenReturn(invalidStock);

        agent = new InstrumentIdentityAgent(yFinService, cache, avProvider(avService));
        var result = agent.resolveIdentity(ticker);

        assertNotNull(result);
        assertEquals("Invalid Company", result.companyName());
        assertEquals("NYSE", result.exchange());
    }

    @Test
    void resolveIdentity_returnsNullWhenBothSourcesFail() throws Exception {
        var ticker = new ResearchTypes.Ticker("INVALID", "");
        AlphaVantageService avService = mock(AlphaVantageService.class);
        when(avService.getOverview("INVALID")).thenReturn(null);

        when(yFinService.getTickerInfo("INVALID")).thenThrow(new RuntimeException("429 Too Many Requests"));

        agent = new InstrumentIdentityAgent(yFinService, cache, avProvider(avService));
        var result = agent.resolveIdentity(ticker);

        assertNull(result);
    }

    @Test
    void resolveIdentity_returnsNullWhenNoAlphaVantageBean() throws Exception {
        var ticker = new ResearchTypes.Ticker("AAPL", "");
        when(yFinService.getTickerInfo("AAPL")).thenThrow(new RuntimeException("429 Too Many Requests"));

        agent = new InstrumentIdentityAgent(yFinService, cache, noAvProvider());
        var result = agent.resolveIdentity(ticker);

        assertNull(result);
    }
}