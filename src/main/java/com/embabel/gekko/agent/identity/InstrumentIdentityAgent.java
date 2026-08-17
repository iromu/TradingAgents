package com.embabel.gekko.agent.identity;

import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.gekko.dataflows.AlphaVantageService;
import com.embabel.gekko.dataflows.YFinService;
import com.embabel.gekko.domain.ResearchTypes;
import com.embabel.gekko.util.PromptSanitizer;
import com.embabel.gekko.util.ResultCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

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
    private final ResultCache resultCache;
    private final ObjectProvider<AlphaVantageService> alphaVantageProvider;

    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 2000;

    @Action(description = "Resolve ticker to real company identity (name, sector, industry, exchange)")
    @AchievesGoal(description = "Resolve a ticker symbol to its real company identity to prevent LLM hallucination")
    public InstrumentContext resolveIdentity(ResearchTypes.Ticker ticker) {
        if (ticker == null || ticker.content() == null || ticker.content().isBlank()) {
            log.warn("Invalid ticker format, skipping identity resolution: {}", ticker);
            return null;
        }

        String tickerUpper = ticker.content().toUpperCase();
        if (!tickerUpper.matches("^[A-Z0-9.\\-]+$")) {
            log.warn("Invalid ticker format, skipping identity resolution: {}", tickerUpper);
            return null;
        }

        String cacheKey = ResultCache.canonicalKey(CACHE_PREFIX, tickerUpper);

        return resultCache.getOrCompute(ResultCache.CATEGORY_EXTERNAL_HTTP, cacheKey, InstrumentContext.class, () -> {
            // Try Yahoo Finance first (with retry)
            try {
                yahoofinance.Stock stock = fetchWithRetry(tickerUpper);

                if (stock == null || !stock.isValid()) {
                    log.warn("No Yahoo Finance info for ticker: {}, trying Alpha Vantage fallback", ticker.content());
                    return tryAlphaVantageFallback(ticker.content(), tickerUpper);
                }

                String companyName = PromptSanitizer.sanitizeForPrompt(stock.getName() != null ? stock.getName() : tickerUpper);
                String sector = "Unknown";
                String industry = "Unknown";
                String exchange = PromptSanitizer.sanitizeForPrompt(stock.getStockExchange() != null ? stock.getStockExchange() : "Unknown");
                String currency = PromptSanitizer.sanitizeForPrompt(stock.getCurrency() != null ? stock.getCurrency() : "USD");

                InstrumentContext context = new InstrumentContext(
                        tickerUpper, companyName, sector, industry, exchange, currency
                );

                log.info("Resolved identity for {}: {} ({}) via Yahoo Finance", tickerUpper, companyName, sector);
                return context;

            } catch (Exception e) {
                log.warn("Yahoo Finance failed for {}, trying Alpha Vantage fallback: {}", tickerUpper, e.getMessage());
                return tryAlphaVantageFallback(tickerUpper, tickerUpper);
            }
        });
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
            String sector = PromptSanitizer.sanitizeForPrompt(overview.getOrDefault("Sector", "Unknown"));
            String industry = PromptSanitizer.sanitizeForPrompt(overview.getOrDefault("Industry", "Unknown"));
            String exchange = PromptSanitizer.sanitizeForPrompt(overview.getOrDefault("Exchange", "Unknown"));
            String currency = PromptSanitizer.sanitizeForPrompt(overview.getOrDefault("Currency", "USD"));

            // Use Name from Alpha Vantage if available, otherwise fall back to ticker display
            String companyName = (name != null && !name.isBlank())
                    ? PromptSanitizer.sanitizeForPrompt(name)
                    : tickerDisplay;

            InstrumentContext context = new InstrumentContext(
                    tickerUpper, companyName, sector, industry, exchange, currency
            );

            log.info("Resolved identity for {}: {} ({}) via Alpha Vantage fallback", tickerUpper, companyName, sector);
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
                        TimeUnit.MILLISECONDS.sleep(backoff);
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
