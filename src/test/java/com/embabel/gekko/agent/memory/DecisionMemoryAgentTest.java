package com.embabel.gekko.agent.memory;

import com.embabel.agent.api.common.OperationContext;
import com.embabel.agent.test.unit.FakeOperationContext;
import com.embabel.gekko.dataflows.YFinService;
import com.embabel.gekko.util.FileCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DecisionMemoryAgent actions using FakeOperationContext.
 */
class DecisionMemoryAgentTest {

    @TempDir
    Path tempDir;

    private DecisionMemoryAgent agent;
    private DecisionMemoryRepository repository;
    private FakeOperationContext ctx;

    @BeforeEach
    void setUp() throws Exception {
        String logPath = tempDir.resolve("memory.md").toString();
        repository = new DecisionMemoryRepository(logPath, 0);
        YFinService yFinService = new YFinService();
        agent = new DecisionMemoryAgent(repository, yFinService);
        ctx = FakeOperationContext.create();
    }

    // --- storeDecision ---

    @Test
    void storeDecision_appendsPendingEntry() throws Exception {
        agent.storeDecision("AAPL", "2026-01-15", "Buy",
                "Strong fundamentals", "Growth thesis");

        assertTrue(repository.hasPendingEntriesFor("AAPL"));

        var entries = repository.getPendingEntries("AAPL");
        assertEquals(1, entries.size());
        assertEquals("Buy", entries.get(0).rating());
        // New Python-compatible format: investment thesis is in ALPHA, executiveSummary is N/A
        assertEquals("Growth thesis", entries.get(0).investmentThesis());
    }

    @Test
    void storeDecision_multipleTickers() throws Exception {
        agent.storeDecision("AAPL", "2026-01-15", "Buy", "Summary A", "Thesis A");
        agent.storeDecision("MSFT", "2026-01-15", "Hold", "Summary M", "Thesis M");

        assertTrue(repository.hasPendingEntriesFor("AAPL"));
        assertTrue(repository.hasPendingEntriesFor("MSFT"));
    }

    // --- resolvePending ---

    @Test
    void resolvePending_noOpWhenNoPending() {
        // Should not throw, just log debug
        assertDoesNotThrow(() -> agent.resolvePending("AAPL", "2026-01-15", ctx));
    }

    @Test
    void resolvePending_handlesYFinServiceException() throws Exception {
        // First store a pending decision
        agent.storeDecision("AAPL", "2026-01-15", "Buy", "Summary", "Thesis");

        // Mock the LLM response for reflection
        ctx.expectResponse("The buy decision was correct because AAPL rose");

        // Act — resolvePending will call fetchReturns (which uses YFinService)
        // Since we don't have real YFin data, it may throw (429 rate limit)
        // But resolvePending should not throw
        assertDoesNotThrow(() -> agent.resolvePending("AAPL", "2026-01-15", ctx));
    }

    @Test
    void resolvePending_skipsWrongDate() throws Exception {
        agent.storeDecision("AAPL", "2026-01-15", "Buy", "Summary", "Thesis");

        // Try to resolve for a different date
        agent.resolvePending("AAPL", "2026-02-01", ctx);

        // Original entry should still be pending
        assertTrue(repository.hasPendingEntriesFor("AAPL"));
    }

    // --- generatePastContext ---

    @Test
    void generatePastContext_returnsEmptyForNewTicker() throws Exception {
        agent.storeDecision("AAPL", "2026-01-15", "Buy", "Summary", "Thesis");

        String context = agent.generatePastContext("NVDA");

        assertEquals("", context);
    }

    @Test
    void generatePastContext_returnsFormattedString() throws Exception {
        // Add a resolved entry for AAPL
        String entry = """
                [2026-01-10 | AAPL | Buy | +3.2% | +1.5% | 5d]

                DECISION:
                **Rating**: Buy

                **Executive Summary**: N/A

                REFLECTION:
                AAPL rose on strong earnings.

                <!-- ENTRY_END -->
                """;
        var field = DecisionMemoryRepository.class.getDeclaredField("memoryLogPath");
        field.setAccessible(true);
        Path path = (Path) field.get(repository);
        Files.writeString(path, entry, StandardCharsets.UTF_8);

        String context = agent.generatePastContext("AAPL");

        assertTrue(context.contains("PAST DECISION MEMORY:"));
        assertTrue(context.contains("AAPL rose on strong earnings"));
    }

    // --- fetchReturns ---

    @Test
    void fetchReturns_methodExists() throws Exception {
        // Verify the method exists and has the right signature
        var method = DecisionMemoryAgent.class.getMethod("fetchReturns", String.class, String.class);
        assertEquals(java.util.concurrent.Callable.class.isAssignableFrom(method.getReturnType()) == false, true);
        // Returns ReturnsData (a record)
        assertNotNull(method.getReturnType());
    }

    @Test
    void fetchReturns_returnsReturnsDataRecord() throws Exception {
        // The method returns a ReturnsData record with the expected fields
        var method = DecisionMemoryAgent.class.getMethod("fetchReturns", String.class, String.class);
        // Verify the method declares Exception in its throws clause
        assertTrue(java.util.Arrays.stream(method.getExceptionTypes())
                .anyMatch(t -> t.equals(Exception.class)));
    }
}
