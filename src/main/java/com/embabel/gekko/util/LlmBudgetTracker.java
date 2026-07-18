package com.embabel.gekko.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Soft limiter for LLM API calls per ticker.
 * Logs a warning when a ticker exceeds its configured budget but does not block calls.
 */
@Slf4j
@Component
public class LlmBudgetTracker {

    private final int budget;
    private final Map<String, Integer> callCounts = new ConcurrentHashMap<>();

    public LlmBudgetTracker(
            @Value("${llm.budget.max:30}") int maxBudget
    ) {
        this.budget = maxBudget;
    }

    /**
     * Record a call for the given ticker. Logs a warning if the budget is exceeded.
     *
     * @param ticker the ticker symbol (e.g., "AAPL")
     * @return the current call count for this ticker
     */
    public int recordCall(String ticker) {
        int count = callCounts.merge(ticker, 1, Integer::sum);
        if (count > budget) {
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