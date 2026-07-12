package com.embabel.gekko.agent.identity;

import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.gekko.dataflows.AlphaVantageService;
import com.embabel.gekko.dataflows.YFinService;
import com.embabel.gekko.domain.ResearchTypes;
import com.embabel.gekko.util.FileCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Resolves a ticker symbol to its real company identity (name, sector, industry, exchange)
 * to prevent LLM hallucination. Uses Yahoo Finance as the primary data source with
 * Alpha Vantage as a fallback when Yahoo Finance is unavailable.
 */
@Agent(description = "Resolves ticker to real company identity to prevent LLM hallucination")
@Component
@RequiredArgsConstructor
@Slf4j
public class InstrumentIdentityAgent {

    private static final String CACHE_PREFIX = "identity:";

    private final YFinService yFinService;
    private final FileCache fileCache;
    private final ObjectProvider<AlphaVantageService> alphaVantageProvider;

    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 2000;

    @Action(description = "Resolve ticker to real company identity (name, sector, industry, exchange)")
    public InstrumentContext resolveIdentity(ResearchTypes.Ticker ticker) {
        if (ticker == null || ticker.content() == null || ticker.content().isBlank()
                || !ticker.content().matches("^[A-Z0-9.\\-]+$")) {
            log.warn("Invalid ticker format, skipping identity resolution: {}", ticker);
            return null;
        }

        String cacheKey = CACHE_PREFIX + ticker.content();

        // Check cache first
        InstrumentContext cached = fileCache.get(cacheKey, InstrumentContext.class);
        if (cached != null) {
            log.debug("Cache hit for identity: {}", ticker.content());
            return cached;
        }

        String tickerUpper = ticker.content().toUpperCase();

        // Try Yahoo Finance first (with retry)
        try {
            yahoofinance.Stock stock = fetchWithRetry(tickerUpper);

            if (stock == null || !stock.isValid()) {
                log.warn("No Yahoo Finance info for ticker: {}, trying Alpha Vantage fallback", ticker.content());
                return tryAlphaVantageFallback(ticker.content(), tickerUpper);
            }

            String companyName = stock.getName() != null ? stock.getName() : ticker.content();
            String sector = "Unknown";
            String industry = "Unknown";
            String exchange = stock.getStockExchange() != null ? stock.getStockExchange() : "Unknown";
            String currency = stock.getCurrency() != null ? stock.getCurrency() : "USD";

            InstrumentContext context = new InstrumentContext(
                    ticker.content(), companyName, sector, industry, exchange, currency
            );

            fileCache.save(cacheKey, context);
            log.info("Resolved identity for {}: {} ({}) via Yahoo Finance", ticker.content(), companyName, sector);
            return context;

        } catch (Exception e) {
            log.warn("Yahoo Finance failed for {}, trying Alpha Vantage fallback: {}", ticker.content(), e.getMessage());
            return tryAlphaVantageFallback(ticker.content(), tickerUpper);
        }
    }

    /**
     * Fallback to Alpha Vantage OVERVIEW when Yahoo Finance fails.
     */
    private InstrumentContext tryAlphaVantageFallback(String tickerDisplay, String tickerUpper) {
        var alphaVantageService = alphaVantageProvider.getIfAvailable();
        if (alphaVantageService == null) {
            log.warn("Alpha Vantage not available — no fallback for {}", tickerDisplay);
            return null;
        }

        try {
            java.util.Map<String, String> overview = alphaVantageService.getOverview(tickerUpper);
            if (overview == null || overview.isEmpty()) {
                log.warn("Alpha Vantage OVERVIEW returned empty for {}", tickerDisplay);
                return null;
            }

            String name = overview.getOrDefault("Name", null);
            String sector = overview.getOrDefault("Sector", "Unknown");
            String industry = overview.getOrDefault("Industry", "Unknown");
            String exchange = overview.getOrDefault("Exchange", "Unknown");
            String currency = overview.getOrDefault("Currency", "USD");

            // Use Name from Alpha Vantage if available, otherwise fall back to ticker display
            String companyName = (name != null && !name.isBlank()) ? name : tickerDisplay;

            InstrumentContext context = new InstrumentContext(
                    tickerDisplay, companyName, sector, industry, exchange, currency
            );

            fileCache.save(CACHE_PREFIX + tickerDisplay, context);
            log.info("Resolved identity for {}: {} ({}) via Alpha Vantage fallback", tickerDisplay, companyName, sector);
            return context;

        } catch (Exception e) {
            log.warn("Alpha Vantage fallback failed for {}: {}", tickerDisplay, e.getMessage());
            return null;
        }
    }

    private yahoofinance.Stock fetchWithRetry(String ticker) throws Exception {
        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return yFinService.getTickerInfo(ticker);
            } catch (Exception e) {
                lastException = e;
                if (attempt < MAX_RETRIES) {
                    long backoff = INITIAL_BACKOFF_MS * (1L << (attempt - 1));
                    log.warn("Yahoo Finance request failed for {} (attempt {}/{}, error: {}), retrying in {}ms",
                            ticker, attempt, MAX_RETRIES, e.getMessage(), backoff);
                    try {
                        Thread.sleep(backoff);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Retry interrupted for ticker " + ticker, ie);
                    }
                }
            }
        }

        throw lastException;
    }
}
