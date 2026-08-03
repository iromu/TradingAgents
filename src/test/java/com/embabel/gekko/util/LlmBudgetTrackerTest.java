package com.embabel.gekko.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LlmBudgetTrackerTest {

    private LlmBudgetTracker tracker;
    private LlmBudgetTracker hardLimitTracker;

    @BeforeEach
    void setUp() {
        tracker = new LlmBudgetTracker(5, false);
        hardLimitTracker = new LlmBudgetTracker(5, true);
    }

    @Test
    void recordCall_incrementsCount() {
        assertEquals(0, tracker.getCallCount("AAPL"));

        int count1 = tracker.recordCall("AAPL");
        assertEquals(1, count1);
        assertEquals(1, tracker.getCallCount("AAPL"));

        int count2 = tracker.recordCall("AAPL");
        assertEquals(2, count2);
        assertEquals(2, tracker.getCallCount("AAPL"));
    }

    @Test
    void recordCall_independentPerTicker() {
        tracker.recordCall("AAPL");
        tracker.recordCall("AAPL");
        tracker.recordCall("GOOGL");

        assertEquals(2, tracker.getCallCount("AAPL"));
        assertEquals(1, tracker.getCallCount("GOOGL"));
    }

    @Test
    void recordCall_logsWarningWhenExceedingSoftBudget() {
        // Fill up to budget (5 calls)
        for (int i = 0; i < 5; i++) {
            tracker.recordCall("AAPL");
        }
        assertEquals(5, tracker.getCallCount("AAPL"));

        // 6th call exceeds budget but does not throw (soft limit)
        int count = tracker.recordCall("AAPL");
        assertEquals(6, count);
    }

    @Test
    void recordCall_throwsWhenExceedingHardLimit() {
        // Fill up to budget (5 calls)
        for (int i = 0; i < 5; i++) {
            hardLimitTracker.recordCall("AAPL");
        }
        assertEquals(5, hardLimitTracker.getCallCount("AAPL"));

        // 6th call exceeds hard limit
        var exception = assertThrows(com.embabel.gekko.util.BudgetExceededException.class,
                () -> hardLimitTracker.recordCall("AAPL"));

        assertEquals("AAPL", exception.getTicker());
        assertEquals(6, exception.getCallCount());
        assertEquals(5, exception.getBudget());
        assertTrue(exception.getMessage().contains("AAPL"));
        assertTrue(exception.getMessage().contains("6"));
        assertTrue(exception.getMessage().contains("5"));
    }

    @Test
    void budgetExceededException_messageContainsDetails() {
        var ex = new com.embabel.gekko.util.BudgetExceededException("TSLA", 10, 8);
        assertTrue(ex.getMessage().contains("TSLA"));
        assertTrue(ex.getMessage().contains("10"));
        assertTrue(ex.getMessage().contains("8"));
        assertEquals("TSLA", ex.getTicker());
        assertEquals(10, ex.getCallCount());
        assertEquals(8, ex.getBudget());
    }

    @Test
    void reset_clearsTickerCount() {
        tracker.recordCall("AAPL");
        tracker.recordCall("AAPL");
        assertEquals(2, tracker.getCallCount("AAPL"));

        tracker.reset("AAPL");
        assertEquals(0, tracker.getCallCount("AAPL"));
    }

    @Test
    void reset_removesUnknownTicker() {
        // Should not throw
        tracker.reset("UNKNOWN");
        assertEquals(0, tracker.getCallCount("UNKNOWN"));
    }

    @Test
    void resetAll_clearsAll() {
        tracker.recordCall("AAPL");
        tracker.recordCall("GOOGL");
        tracker.recordCall("MSFT");

        assertEquals(1, tracker.getCallCount("AAPL"));
        assertEquals(1, tracker.getCallCount("GOOGL"));
        assertEquals(1, tracker.getCallCount("MSFT"));

        tracker.resetAll();

        assertEquals(0, tracker.getCallCount("AAPL"));
        assertEquals(0, tracker.getCallCount("GOOGL"));
        assertEquals(0, tracker.getCallCount("MSFT"));
    }

    @Test
    void getCallCount_returnsZeroForUnknown() {
        assertEquals(0, tracker.getCallCount("NONEXISTENT"));
    }
}