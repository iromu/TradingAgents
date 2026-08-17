package com.embabel.gekko.agent.identity;

import com.embabel.gekko.dataflows.AlphaVantageService;
import com.embabel.gekko.dataflows.YFinService;
import com.embabel.gekko.domain.ResearchTypes;
import com.embabel.gekko.util.FileCache;
import com.embabel.gekko.util.ResultCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.ObjectProvider;

import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for InstrumentIdentityAgent.
 */
class InstrumentIdentityAgentTest {

    @TempDir
    Path tempDir;

    private ResultCache resultCache;
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
        var fileCache = new FileCache();
        try {
            var field = FileCache.class.getDeclaredField("baseDir");
            field.setAccessible(true);
            field.set(fileCache, tempDir);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set cache baseDir", e);
        }
        resultCache = new ResultCache(fileCache, "5m", "1h");
        yFinService = mock(YFinService.class);
        agent = new InstrumentIdentityAgent(yFinService, resultCache, noAvProvider());
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

        agent = new InstrumentIdentityAgent(yFinService, resultCache, avProvider(avService));
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

        agent = new InstrumentIdentityAgent(yFinService, resultCache, avProvider(avService));
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

        agent = new InstrumentIdentityAgent(yFinService, resultCache, avProvider(avService));
        var result = agent.resolveIdentity(ticker);

        assertNull(result);
    }

    @Test
    void resolveIdentity_returnsNullWhenNoAlphaVantageBean() throws Exception {
        var ticker = new ResearchTypes.Ticker("AAPL", "");
        when(yFinService.getTickerInfo("AAPL")).thenThrow(new RuntimeException("429 Too Many Requests"));

        agent = new InstrumentIdentityAgent(yFinService, resultCache, noAvProvider());
        var result = agent.resolveIdentity(ticker);

        assertNull(result);
    }

    @Test
    void resolveIdentity_normalizesLowercaseTicker() throws Exception {
        var ticker = new ResearchTypes.Ticker("aapl", "");
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
        verify(yFinService).getTickerInfo("AAPL");
    }

    @Test
    void resolveIdentity_cacheHitAcrossCaseVariants() throws Exception {
        yahoofinance.Stock stock = mock(yahoofinance.Stock.class);
        when(stock.isValid()).thenReturn(true);
        when(stock.getName()).thenReturn("Apple Inc.");
        when(stock.getStockExchange()).thenReturn("NASDAQ");
        when(stock.getCurrency()).thenReturn("USD");
        when(yFinService.getTickerInfo(any(String.class))).thenReturn(stock);

        // First call with lowercase
        var result1 = agent.resolveIdentity(new ResearchTypes.Ticker("aapl", ""));
        assertNotNull(result1);
        assertEquals("AAPL", result1.ticker());

        // Second call with mixed case — should also work (cache or fresh fetch)
        var result2 = agent.resolveIdentity(new ResearchTypes.Ticker("aApL", ""));
        assertNotNull(result2);
        assertEquals("AAPL", result2.ticker());

        // Third call with uppercase
        var result3 = agent.resolveIdentity(new ResearchTypes.Ticker("AAPL", ""));
        assertNotNull(result3);
        assertEquals("AAPL", result3.ticker());

        // All results should have the same normalized ticker
        assertEquals(result1.ticker(), result2.ticker());
        assertEquals(result2.ticker(), result3.ticker());
    }

    // --- Identity sanitization before caching (Task 5.2) ---

    @Test
    void resolveIdentity_sanitizesCompanyNameBeforeCaching() throws Exception {
        var ticker = new ResearchTypes.Ticker("EVIL", "");
        yahoofinance.Stock stock = mock(yahoofinance.Stock.class);
        when(stock.isValid()).thenReturn(true);
        when(stock.getName()).thenReturn("Evil Corp {{ config.secret }}");
        when(stock.getStockExchange()).thenReturn("NYSE<script>alert(1)</script>");
        when(stock.getCurrency()).thenReturn("USD");
        when(yFinService.getTickerInfo("EVIL")).thenReturn(stock);

        var result = agent.resolveIdentity(ticker);

        assertNotNull(result);
        assertThat(result.companyName()).doesNotContain("{{").doesNotContain("}}");
        assertThat(result.exchange()).doesNotContain("<script");
    }

    @Test
    void resolveIdentity_sanitizesAlphaVantageFallbackFields() throws Exception {
        var ticker = new ResearchTypes.Ticker("MALICIOUS", "");
        AlphaVantageService avService = mock(AlphaVantageService.class);
        when(avService.getOverview("MALICIOUS")).thenReturn(Map.of(
                "Name", "Bad Co {% set x = 1 %}",
                "Sector", "Tech<div onclick='evil()'>",
                "Industry", "Finance",
                "Exchange", "NASDAQ",
                "Currency", "USD"
        ));
        when(yFinService.getTickerInfo("MALICIOUS")).thenThrow(new RuntimeException("down"));

        agent = new InstrumentIdentityAgent(yFinService, resultCache, avProvider(avService));
        var result = agent.resolveIdentity(ticker);

        assertNotNull(result);
        assertThat(result.companyName()).doesNotContain("{%").doesNotContain("%}");
        assertThat(result.sector()).doesNotContain("onclick=");
    }
}