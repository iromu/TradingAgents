package com.embabel.gekko.agent.memory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DecisionMemoryRepository.
 * Tests append, parse, resolve, generatePastContext, rotation, atomic writes, and corruption recovery.
 */
class DecisionMemoryRepositoryTest {

    @TempDir
    Path tempDir;

    private DecisionMemoryRepository createRepository(String logPath, int maxEntries) {
        return new DecisionMemoryRepository(logPath, maxEntries);
    }

    private String readLog(DecisionMemoryRepository repo) throws Exception {
        var field = DecisionMemoryRepository.class.getDeclaredField("memoryLogPath");
        field.setAccessible(true);
        Path path = (Path) field.get(repo);
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    // --- append pending ---

    @Test
    void appendPending_createsEntry() throws Exception {
        var repo = createRepository(tempDir.resolve("memory.md").toString(), 0);
        repo.appendPending("AAPL", "2026-01-15", "Buy", "Strong fundamentals", "Growth thesis");

        String content = readLog(repo);
        assertTrue(content.contains("[2026-01-15 | AAPL | Buy | pending]"));
        assertTrue(content.contains("DECISION: AAPL"));
        assertTrue(content.contains("RATING: Buy"));
        assertTrue(content.contains("DATE: 2026-01-15"));
        assertTrue(content.contains("ALPHA: Growth thesis"));
        assertTrue(content.contains("<!-- ENTRY_END -->"));
    }

    @Test
    void appendPending_multipleEntries() throws Exception {
        var repo = createRepository(tempDir.resolve("memory.md").toString(), 0);
        repo.appendPending("AAPL", "2026-01-15", "Buy", "Summary 1", "Thesis 1");
        repo.appendPending("MSFT", "2026-01-16", "Hold", "Summary 2", "Thesis 2");

        String content = readLog(repo);
        assertTrue(content.contains("[2026-01-15 | AAPL | Buy | pending]"));
        assertTrue(content.contains("[2026-01-16 | MSFT | Hold | pending]"));
    }

    // --- parse pending ---

    @Test
    void getPendingEntries_parsesAAPL() throws Exception {
        var repo = createRepository(tempDir.resolve("memory.md").toString(), 0);
        repo.appendPending("AAPL", "2026-01-15", "Buy", "Strong fundamentals", "Growth thesis");

        var entries = repo.getPendingEntries("AAPL");

        assertEquals(1, entries.size());
        assertEquals("AAPL", entries.get(0).ticker());
        assertEquals("2026-01-15", entries.get(0).tradeDate());
        assertEquals("Buy", entries.get(0).rating());
        // New Python-compatible format doesn't have executive summary field
        assertEquals("N/A", entries.get(0).executiveSummary());
        assertEquals("Growth thesis", entries.get(0).investmentThesis());
    }

    @Test
    void getPendingEntries_returnsEmptyForUnknownTicker() throws Exception {
        var repo = createRepository(tempDir.resolve("memory.md").toString(), 0);
        repo.appendPending("AAPL", "2026-01-15", "Buy", "Summary", "Thesis");

        var entries = repo.getPendingEntries("MSFT");

        assertTrue(entries.isEmpty());
    }

    @Test
    void hasPendingEntriesFor_returnsTrue() throws Exception {
        var repo = createRepository(tempDir.resolve("memory.md").toString(), 0);
        repo.appendPending("AAPL", "2026-01-15", "Buy", "Summary", "Thesis");

        assertTrue(repo.hasPendingEntriesFor("AAPL"));
    }

    @Test
    void hasPendingEntriesFor_returnsFalseForUnknown() throws Exception {
        var repo = createRepository(tempDir.resolve("memory.md").toString(), 0);
        repo.appendPending("AAPL", "2026-01-15", "Buy", "Summary", "Thesis");

        assertFalse(repo.hasPendingEntriesFor("MSFT"));
    }

    // --- resolve ---

    @Test
    void resolve_updatesPendingToResolved() throws Exception {
        var repo = createRepository(tempDir.resolve("memory.md").toString(), 0);
        repo.appendPending("AAPL", "2026-01-15", "Buy", "Strong fundamentals", "Growth thesis");

        repo.resolve("AAPL", "2026-01-15",
                BigDecimal.valueOf(5.2), BigDecimal.valueOf(3.1), "SPY", 5,
                "The buy decision was correct as AAPL rose 5.2%");

        String content = readLog(repo);
        // Should no longer contain "pending"
        assertFalse(content.contains("pending"));
        // Should contain resolved header format
        assertTrue(content.contains("[2026-01-15 | AAPL | Buy | 5.2 | 3.1 | 5d]"));
        // Should contain reflection
        assertTrue(content.contains("REFLECTION:"));
        assertTrue(content.contains("The buy decision was correct"));
    }

    @Test
    void resolve_noOpForUnknownTicker() throws Exception {
        var repo = createRepository(tempDir.resolve("memory.md").toString(), 0);
        repo.appendPending("AAPL", "2026-01-15", "Buy", "Summary", "Thesis");

        // Resolve for different ticker — should not modify file
        repo.resolve("MSFT", "2026-01-15",
                BigDecimal.valueOf(1.0), BigDecimal.valueOf(0.5), "SPY", 5,
                "Reflection");

        String content = readLog(repo);
        assertTrue(content.contains("pending"));
    }

    // --- generate past context ---

    @Test
    void generatePastContext_returnsEmptyForNewTicker() throws Exception {
        var repo = createRepository(tempDir.resolve("memory.md").toString(), 0);
        repo.appendPending("AAPL", "2026-01-15", "Buy", "Summary", "Thesis");

        String context = repo.generatePastContext("NVDA");

        assertEquals("", context);
    }

    @Test
    void generatePastContext_includesSameTickerLessons() throws Exception {
        var repo = createRepository(tempDir.resolve("memory.md").toString(), 0);
        // Add resolved entries for AAPL
        for (int i = 1; i <= 3; i++) {
            String entry = String.format(
                    """
                    [2026-01-%02d | AAPL | Buy | +%.1f%% | +%.1f%% | 5d]

                    DECISION:
                    **Rating**: Buy

                    **Executive Summary**: N/A

                    REFLECTION:
                    AAPL lesson %d.

                    <!-- ENTRY_END -->
                    """, 10 + i, i * 2.0, i * 1.0, i);
            var field = DecisionMemoryRepository.class.getDeclaredField("memoryLogPath");
            field.setAccessible(true);
            Path path = (Path) field.get(repo);
            Files.writeString(path, entry + "\n", StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.APPEND);
        }

        String context = repo.generatePastContext("AAPL");

        assertTrue(context.contains("PAST DECISION MEMORY:"));
        assertTrue(context.contains("AAPL lesson 1"));
        assertTrue(context.contains("AAPL lesson 2"));
        assertTrue(context.contains("AAPL lesson 3"));
    }

    @Test
    void generatePastContext_limitsSameTickerToFive() throws Exception {
        var repo = createRepository(tempDir.resolve("memory.md").toString(), 0);
        // Add 7 resolved entries for AAPL
        for (int i = 1; i <= 7; i++) {
            String entry = String.format(
                    """
                    [2026-01-%02d | AAPL | Buy | +%.1f%% | +%.1f%% | 5d]

                    DECISION:
                    **Rating**: Buy

                    **Executive Summary**: N/A

                    REFLECTION:
                    AAPL lesson %d.

                    <!-- ENTRY_END -->
                    """, 1 + (i % 28), i * 1.0, i * 0.5, i);
            var field = DecisionMemoryRepository.class.getDeclaredField("memoryLogPath");
            field.setAccessible(true);
            Path path = (Path) field.get(repo);
            Files.writeString(path, entry + "\n", StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.APPEND);
        }

        String context = repo.generatePastContext("AAPL");

        // Should only include 5 most recent (lessons 3-7)
        int count = 0;
        for (int i = 1; i <= 7; i++) {
            if (context.contains("AAPL lesson " + i)) count++;
        }
        assertEquals(5, count);
    }

    @Test
    void generatePastContext_includesCrossTickerLessons() throws Exception {
        var repo = createRepository(tempDir.resolve("memory.md").toString(), 0);
        // Add resolved entries for other tickers
        String[] tickers = {"MSFT", "GOOGL", "AMZN", "META", "TSLA"};
        for (int i = 0; i < 5; i++) {
            String entry = String.format(
                    """
                    [2026-01-%02d | %s | Hold | +%.1f%% | +%.1f%% | 5d]

                    DECISION:
                    **Rating**: Hold

                    **Executive Summary**: N/A

                    REFLECTION:
                    %s lesson %d.

                    <!-- ENTRY_END -->
                    """, i + 1, tickers[i], (i + 1) * 0.5, (i + 1) * 0.3, tickers[i], i + 1);
            var field = DecisionMemoryRepository.class.getDeclaredField("memoryLogPath");
            field.setAccessible(true);
            Path path = (Path) field.get(repo);
            Files.writeString(path, entry + "\n", StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.APPEND);
        }

        String context = repo.generatePastContext("NVDA");

        // Should include up to 3 cross-ticker
        int count = 0;
        for (String t : tickers) {
            if (context.contains(t + " lesson")) count++;
        }
        assertEquals(3, count);
    }

    // --- rotation ---

    @Test
    void rotate_prunesOldestWhenOverLimit() throws Exception {
        var repo = createRepository(tempDir.resolve("memory.md").toString(), 2);
        for (int i = 1; i <= 4; i++) {
            String entry = String.format(
                    """
                    [2026-01-%02d | AAPL | Buy | +%.1f%% | +%.1f%% | 5d]

                    DECISION:
                    **Rating**: Buy

                    **Executive Summary**: N/A

                    REFLECTION:
                    Lesson %d.

                    <!-- ENTRY_END -->
                    """, i, i * 1.0, i * 0.5, i);
            var field = DecisionMemoryRepository.class.getDeclaredField("memoryLogPath");
            field.setAccessible(true);
            Path path = (Path) field.get(repo);
            Files.writeString(path, entry + "\n", StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.APPEND);
        }

        repo.rotate();

        String result = readLog(repo);
        assertTrue(result.contains("Lesson 3"));
        assertTrue(result.contains("Lesson 4"));
        assertFalse(result.contains("Lesson 1"));
        assertFalse(result.contains("Lesson 2"));
    }

    @Test
    void rotate_noOpWhenUnderLimit() throws Exception {
        var repo = createRepository(tempDir.resolve("memory.md").toString(), 10);
        for (int i = 1; i <= 3; i++) {
            String entry = String.format(
                    """
                    [2026-01-%02d | AAPL | Buy | +%.1f%% | +%.1f%% | 5d]

                    DECISION:
                    **Rating**: Buy

                    **Executive Summary**: N/A

                    REFLECTION:
                    Lesson %d.

                    <!-- ENTRY_END -->
                    """, i, i * 1.0, i * 0.5, i);
            var field = DecisionMemoryRepository.class.getDeclaredField("memoryLogPath");
            field.setAccessible(true);
            Path path = (Path) field.get(repo);
            Files.writeString(path, entry + "\n", StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.APPEND);
        }

        repo.rotate();

        String result = readLog(repo);
        assertTrue(result.contains("Lesson 1"));
        assertTrue(result.contains("Lesson 2"));
        assertTrue(result.contains("Lesson 3"));
    }

    @Test
    void rotate_noOpWhenMaxEntriesZero() throws Exception {
        var repo = createRepository(tempDir.resolve("memory.md").toString(), 0);
        for (int i = 1; i <= 10; i++) {
            String entry = String.format(
                    """
                    [2026-01-%02d | AAPL | Buy | +%.1f%% | +%.1f%% | 5d]

                    DECISION:
                    **Rating**: Buy

                    **Executive Summary**: N/A

                    REFLECTION:
                    Lesson %d.

                    <!-- ENTRY_END -->
                    """, i, i * 1.0, i * 0.5, i);
            var field = DecisionMemoryRepository.class.getDeclaredField("memoryLogPath");
            field.setAccessible(true);
            Path path = (Path) field.get(repo);
            Files.writeString(path, entry + "\n", StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.APPEND);
        }

        repo.rotate();

        String result = readLog(repo);
        assertTrue(result.contains("Lesson 1"));
        assertTrue(result.contains("Lesson 10"));
    }

    @Test
    void rotate_preservesPendingEntries() throws Exception {
        var repo = createRepository(tempDir.resolve("memory.md").toString(), 2);
        // Add 3 resolved entries
        for (int i = 1; i <= 3; i++) {
            String entry = String.format(
                    """
                    [2026-01-%02d | AAPL | Buy | +%.1f%% | +%.1f%% | 5d]

                    DECISION:
                    **Rating**: Buy

                    **Executive Summary**: N/A

                    REFLECTION:
                    Resolved lesson %d.

                    <!-- ENTRY_END -->
                    """, i, i * 1.0, i * 0.5, i);
            var field = DecisionMemoryRepository.class.getDeclaredField("memoryLogPath");
            field.setAccessible(true);
            Path path = (Path) field.get(repo);
            Files.writeString(path, entry + "\n", StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.APPEND);
        }
        // Add a pending entry (should be preserved)
        repo.appendPending("AAPL", "2026-01-15", "Buy", "Summary", "Thesis");

        repo.rotate();

        String result = readLog(repo);
        // Pending entry should still be there
        assertTrue(result.contains("pending"));
        // Should only keep 2 resolved entries
        assertTrue(result.contains("Resolved lesson 2"));
        assertTrue(result.contains("Resolved lesson 3"));
        assertFalse(result.contains("Resolved lesson 1"));
    }

    // --- atomic write ---

    @Test
    void appendPending_atomicWrite_multipleEntries() throws Exception {
        var repo = createRepository(tempDir.resolve("memory.md").toString(), 0);
        for (int i = 0; i < 5; i++) {
            repo.appendPending("AAPL", "2026-01-" + (15 + i), "Buy", "Summary " + i, "Thesis " + i);
        }

        String content = readLog(repo);
        for (int i = 0; i < 5; i++) {
            // New format includes thesis in ALPHA field
            assertTrue(content.contains("Thesis " + i));
        }
    }

    // --- corruption recovery ---

    @Test
    void recoverFromCorruption_truncatesToLastCompleteEntry() throws Exception {
        var repo = createRepository(tempDir.resolve("memory.md").toString(), 0);
        String content = """
                [2026-01-15 | AAPL | Buy | pending]

                DECISION:
                **Rating**: Buy

                **Executive Summary**: Test

                **Investment Thesis**: Test

                <!-- ENTRY_END -->
                [2026-01-16 | MSFT | Hold | pen
                """;
        var field = DecisionMemoryRepository.class.getDeclaredField("memoryLogPath");
        field.setAccessible(true);
        Path path = (Path) field.get(repo);
        Files.writeString(path, content, StandardCharsets.UTF_8);

        repo.recoverFromCorruption();

        String recovered = readLog(repo);
        assertTrue(recovered.contains("[2026-01-15 | AAPL | Buy | pending]"));
        assertFalse(recovered.contains("[2026-01-16 | MSFT"));
    }

    @Test
    void recoverFromCorruption_noOpWhenNoCorruption() throws Exception {
        var repo = createRepository(tempDir.resolve("memory.md").toString(), 0);
        String content = """
                [2026-01-15 | AAPL | Buy | pending]

                DECISION:
                **Rating**: Buy

                **Executive Summary**: Test

                **Investment Thesis**: Test

                <!-- ENTRY_END -->
                """;
        var field = DecisionMemoryRepository.class.getDeclaredField("memoryLogPath");
        field.setAccessible(true);
        Path path = (Path) field.get(repo);
        Files.writeString(path, content, StandardCharsets.UTF_8);

        repo.recoverFromCorruption();

        String recovered = readLog(repo);
        assertEquals(content.trim(), recovered.trim());
    }
}
