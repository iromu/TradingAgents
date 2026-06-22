package com.embabel.gekko.agent.identity;

import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.gekko.dataflows.YFinService;
import com.embabel.gekko.domain.ResearchTypes;
import com.embabel.gekko.util.FileCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Resolves a ticker symbol to its real company identity (name, sector, industry, exchange)
 * to prevent LLM hallucination. Uses Yahoo Finance for data and FileCache for caching.
 */
@Agent(description = "Resolves ticker to real company identity to prevent LLM hallucination")
@Component
@RequiredArgsConstructor
@Slf4j
public class InstrumentIdentityAgent {

    private static final String CACHE_PREFIX = "identity:";

    private final YFinService yFinService;
    private final FileCache fileCache;

    @Action(description = "Resolve ticker to real company identity (name, sector, industry, exchange)")
    public InstrumentContext resolveIdentity(ResearchTypes.Ticker ticker) {
        if (!validateTicker(ticker)) {
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

        try {
            // Fetch from Yahoo Finance via YFinService
            yahoofinance.Stock stock = yFinService.getTickerInfo(ticker.content().toUpperCase());

            if (stock == null || !stock.isValid()) {
                log.warn("No Yahoo Finance info for ticker: {}", ticker.content());
                return null;
            }

            String companyName = stock.getName() != null ? stock.getName() : ticker.content();
            String sector = "Unknown";
            String industry = "Unknown";
            String exchange = stock.getStockExchange() != null ? stock.getStockExchange() : "Unknown";
            String currency = stock.getCurrency() != null ? stock.getCurrency() : "USD";

            InstrumentContext context = new InstrumentContext(
                    ticker.content(), companyName, sector, industry, exchange, currency
            );

            // Cache the result
            fileCache.save(cacheKey, context);

            log.info("Resolved identity for {}: {} ({})", ticker.content(), companyName, sector);
            return context;

        } catch (Exception e) {
            log.warn("Failed to resolve identity for {}: {}", ticker.content(), e.getMessage());
            return null;
        }
    }

    boolean validateTicker(ResearchTypes.Ticker ticker) {
        if (ticker == null || ticker.content() == null || ticker.content().isBlank()) {
            return false;
        }
        return ticker.content().matches("^[A-Z0-9.\\-]+$");
    }
}
