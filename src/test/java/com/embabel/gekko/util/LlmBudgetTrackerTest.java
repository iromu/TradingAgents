package com.embabel.gekko.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LlmBudgetTrackerTest {

    private LlmBudgetTracker tracker;
    private LlmBudgetTracker hardLimitTracker;

    @BeforeEach
    void setUp() {
        tracker = new LlmBudgetTracker(5, false, 1000);
        hardLimitTracker = new LlmBudgetTracker(5, true, 1000);
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

    // --- LRU eviction tests ---

    @Test
    void evictsOldestEntryWhenMaxTickersExceeded() {
        var smallTracker = new LlmBudgetTracker(100, false, 3);

        smallTracker.recordCall("A");
        smallTracker.recordCall("B");
        smallTracker.recordCall("C");
        assertEquals(1, smallTracker.getCallCount("A"));
        assertEquals(1, smallTracker.getCallCount("B"));
        assertEquals(1, smallTracker.getCallCount("C"));

        // 4th ticker evicts the least-recently-accessed ("A")
        smallTracker.recordCall("D");
        assertEquals(0, smallTracker.getCallCount("A")); // evicted
        assertEquals(1, smallTracker.getCallCount("D"));
    }

    @Test
    void evictionUsesAccessOrderNotInsertionOrder() {
        var smallTracker = new LlmBudgetTracker(100, false, 3);

        smallTracker.recordCall("A");
        smallTracker.recordCall("B");
        smallTracker.recordCall("C");

        // Access "A" to make it recently used
        smallTracker.getCallCount("A");

        // "B" is now the least-recently-accessed and should be evicted
        smallTracker.recordCall("D");
        assertEquals(1, smallTracker.getCallCount("A")); // survived (recently accessed)
        assertEquals(0, smallTracker.getCallCount("B")); // evicted
        assertEquals(1, smallTracker.getCallCount("C"));
    }

    @Test
    void repeatedEvictionDoesNotLeakMemory() {
        var smallTracker = new LlmBudgetTracker(100, false, 5);

        // Insert 100 tickers into a map that holds only 5
        for (int i = 0; i < 100; i++) {
            smallTracker.recordCall("TICKER_" + i);
        }

        // Oldest entries should be gone
        assertEquals(0, smallTracker.getCallCount("TICKER_0"));
        assertEquals(0, smallTracker.getCallCount("TICKER_10"));
        // Newest entries should remain
        assertEquals(1, smallTracker.getCallCount("TICKER_99"));
        assertEquals(1, smallTracker.getCallCount("TICKER_95"));
    }

    @Test
    void resetAll_clearsEvictionMap() {
        var smallTracker = new LlmBudgetTracker(100, false, 3);
        smallTracker.recordCall("A");
        smallTracker.recordCall("B");
        smallTracker.resetAll();
        assertEquals(0, smallTracker.getCallCount("A"));
        assertEquals(0, smallTracker.getCallCount("B"));
    }
}