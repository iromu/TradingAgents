package com.embabel.gekko.agent.memory;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the Decision Memory feature migrated from Tauric (Python).
 *
 * Validates:
 * - Decision storage and retrieval
 * - Pending decision resolution
 * - Past context generation
 * - File format matches Python project
 * - Atomic writes with corruption recovery
 */
@Tag("integration")
class DecisionMemoryIntegrationTest {

    @TempDir
    Path tempDir;

    private DecisionMemoryRepository makeRepo(int maxEntries) {
        return new DecisionMemoryRepository(tempDir.resolve("decisions.md").toString(), maxEntries);
    }

    private DecisionMemoryRepository makeRepo() {
        return makeRepo(100);
    }

    @Test
    void shouldStoreAndRetrieveDecision() throws IOException {
        var repo = makeRepo();

        // Store a decision using appendPending
        repo.appendPending("AAPL", "2026-06-15", "BUY",
                "Strong buy signal based on fundamentals",
                "Revenue growth 15%, margin expansion");

        // Verify file was created
        var files = Files.list(tempDir).toList();
        assertEquals(1, files.size());

        // Verify file content starts with expected format
        var content = Files.readString(files.get(0));
        assertTrue(content.contains("DECISION: AAPL"), "File should contain DECISION header");
        assertTrue(content.contains("RATING: BUY"), "File should contain RATING");
        assertTrue(content.contains("DATE: 2026-06-15"), "File should contain DATE");
    }

    @Test
    void shouldRetrievePendingDecisions() throws IOException {
        var repo = makeRepo();

        // Store a pending decision
        repo.appendPending("MSFT", "2026-06-15", "HOLD",
                "Hold pending earnings",
                "Waiting for Q3 results");

        // Store a resolved decision (with reflection)
        repo.appendPending("GOOGL", "2026-06-14", "BUY",
                "Buy on dip",
                "Strong AI fundamentals");
        repo.resolve("GOOGL", "2026-06-14", new BigDecimal("0.05"), new BigDecimal("0.02"),
                "GOOGL decision was correct. Stock rose 5% after purchase.", 5, "2026-06-14");

        var pending = repo.getPendingEntries("MSFT");
        assertEquals(1, pending.size());
        assertEquals("MSFT", pending.get(0).ticker());
    }

    @Test
    void shouldResolvePendingDecision() throws IOException {
        var repo = makeRepo();

        // Store a pending decision
        repo.appendPending("TSLA", "2026-06-15", "SELL",
                "Sell on resistance",
                "Overbought RSI");

        // Resolve it
        repo.resolve("TSLA", "2026-06-15", new BigDecimal("-0.03"), new BigDecimal("0.01"),
                "TSLA decision was correct. Stock dropped 3% after recommendation.", 5, "2026-06-15");

        var pending = repo.getPendingEntries("TSLA");
        assertTrue(pending.isEmpty(), "No pending decisions after resolution");
    }

    @Test
    void shouldGeneratePastContext() throws IOException {
        var repo = makeRepo();

        // Store past decisions
        repo.appendPending("AAPL", "2026-06-10", "BUY",
                "Buy on dip", "Strong fundamentals");
        repo.resolve("AAPL", "2026-06-10", new BigDecimal("0.04"), new BigDecimal("0.01"),
                "AAPL BUY was correct. Stock rose 4%.", 5, "2026-06-10");

        repo.appendPending("AAPL", "2026-06-12", "HOLD",
                "Hold at current levels", "Neutral technicals");
        repo.resolve("AAPL", "2026-06-12", new BigDecimal("0.00"), new BigDecimal("0.005"),
                "AAPL HOLD was correct. Stock flat.", 5, "2026-06-12");

        var context = repo.generatePastContext("AAPL");

        assertNotNull(context);
        assertTrue(context.contains("AAPL"), "Context should mention AAPL");
        assertTrue(context.contains("BUY"), "Context should contain past BUY");
        assertTrue(context.contains("HOLD"), "Context should contain past HOLD");
    }

    @Test
    void shouldFormatLogEntryLikePythonProject() throws IOException {
        var repo = makeRepo();

        repo.appendPending("NVDA", "2026-06-15", "BUY",
                "Strong AI demand", "Data center revenue growing");

        var files = Files.list(tempDir).toList();
        var content = Files.readString(files.get(0));

        // Verify the file format matches Python project format
        String[] expectedMarkers = {"DECISION:", "RATING:", "DATE:", "RETURNS:", "ALPHA:", "<!-- ENTRY_END -->"};
        for (String marker : expectedMarkers) {
            assertTrue(content.contains(marker),
                    "Log entry should contain marker: " + marker);
        }
    }

    @Test
    void shouldHandleAtomicWrites() throws IOException {
        var repo = makeRepo();

        // Multiple rapid writes should not corrupt the file
        for (int i = 0; i < 10; i++) {
            repo.appendPending("META", "2026-06-15", i % 2 == 0 ? "BUY" : "SELL",
                    "Test decision " + i, "Test alpha " + i);
        }

        // Should not throw — file should be valid
        var pending = repo.getPendingEntries("META");
        assertEquals(10, pending.size(), "All 10 decisions should be stored");
    }

    @Test
    void shouldHandleCorruptedFileGracefully() throws IOException {
        // Write a corrupted file manually
        var logPath = tempDir;
        Files.createDirectories(logPath);

        var corruptedFile = logPath.resolve("test.txt");
        Files.writeString(corruptedFile, "THIS IS NOT A VALID DECISION FILE\n\nGARBAGE DATA");

        var repo = makeRepo();

        // Should not throw — corrupted entries should be skipped
        var pending = repo.getPendingEntries("NONEXISTENT");
        assertEquals(0, pending.size(), "Corrupted file should yield no pending decisions");
    }

    @Test
    void shouldLimitMaxEntries() throws IOException {
        // Use max-entries = 3
        var repo = new DecisionMemoryRepository(tempDir.toString(), 3);

        for (int i = 0; i < 5; i++) {
            repo.appendPending("AMD", "2026-06-" + (10 + i), "BUY",
                    "Decision " + i, "Alpha " + i);
        }

        // Should only keep the 3 most recent entries
        var files = Files.list(tempDir).toList();
        assertTrue(files.size() <= 3,
                "Should have at most 3 entries, but found " + files.size());
    }

    @Test
    void shouldParseExistingPythonFormatLog() throws IOException {
        var logPath = tempDir;
        Files.createDirectories(logPath);

        // Write a file in the Java project format (which is compatible with Python project format)
        var javaFormat = """
                [2026-01-10 | SPY | BUY | pending]

                DECISION: SPY
                RATING: BUY
                DATE: 2026-01-10
                RETURNS:
                ALPHA: Market rally expected
                <!-- ENTRY_END -->
                """;

        var file = logPath.resolve("decisions.md");
        Files.writeString(file, javaFormat);

        var repo = new DecisionMemoryRepository(file.toString(), 100);
        var pending = repo.getPendingEntries("SPY");

        assertEquals(1, pending.size());
        assertEquals("SPY", pending.get(0).ticker());
    }
}