package com.embabel.gekko.util;

/**
 * Exception thrown when LLM call budget hard limit is exceeded.
 */
public class BudgetExceededException extends RuntimeException {

    private final String ticker;
    private final int callCount;
    private final int budget;

    public BudgetExceededException(String ticker, int callCount, int budget) {
        super("LLM call budget hard limit exceeded for %s: %d calls (budget: %d)".formatted(ticker, callCount, budget));
        this.ticker = ticker;
        this.callCount = callCount;
        this.budget = budget;
    }

    public String getTicker() {
        return ticker;
    }

    public int getCallCount() {
        return callCount;
    }

    public int getBudget() {
        return budget;
    }
}
