package com.embabel.gekko.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Limiter for LLM API calls per ticker.
 * Logs a warning when a ticker exceeds its soft budget limit.
 * Optionally enforces a hard limit that throws {@link BudgetExceededException}.
 * <p>
 * Uses an LRU map capped at {@code maxTickers} entries; the least-recently-accessed
 * ticker is evicted when the cap is reached to prevent unbounded memory growth.
 */
@Slf4j
@Component
public class LlmBudgetTracker {

    private final int budget;
    private final boolean hardLimit;
    private final int maxTickers;
    private final Map<String, Integer> callCounts;

    public LlmBudgetTracker(
            @Value("${llm.budget.max:30}") int maxBudget,
            @Value("${llm.budget.hard-limit:false}") boolean hardLimit,
            @Value("${llm.budget.max-tickers:1000}") int maxTickers
    ) {
        this.budget = maxBudget;
        this.hardLimit = hardLimit;
        this.maxTickers = maxTickers;
        this.callCounts = Collections.synchronizedMap(
                new LinkedHashMap<>(16, 0.75f, true) { // access-order LRU
                    @Override
                    protected boolean removeEldestEntry(Map.Entry<String, Integer> eldest) {
                        return size() > maxTickers;
                    }
                });
    }

    /**
     * Record a call for the given ticker. Logs a warning if the soft budget is exceeded.
     * Throws {@link BudgetExceededException} if hard limit is enabled and exceeded.
     *
     * @param ticker the ticker symbol (e.g., "AAPL")
     * @return the current call count for this ticker
     * @throws BudgetExceededException if hard limit is enabled and budget is exceeded
     */
    public int recordCall(String ticker) {
        int count = callCounts.merge(ticker, 1, Integer::sum);
        if (count > budget) {
            if (hardLimit) {
                throw new BudgetExceededException(ticker, count, budget);
            }
            log.warn("LLM call budget exceeded for {}: {} calls (budget: {})", ticker, count, budget);
        }
        return count;
    }

    /**
     * Get the current call count for a ticker.
     */
    public int getCallCount(String ticker) {
        return callCounts.getOrDefault(ticker, 0);
    }

    /**
     * Reset the call count for a ticker (e.g., after a workflow completes).
     */
    public void reset(String ticker) {
        callCounts.remove(ticker);
    }

    /**
     * Reset all call counts.
     */
    public void resetAll() {
        callCounts.clear();
    }
}