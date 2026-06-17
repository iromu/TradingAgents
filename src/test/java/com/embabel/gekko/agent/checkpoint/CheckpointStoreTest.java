package com.embabel.gekko.agent.checkpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CheckpointStore — save, restore, JSON serialization, atomic writes.
 */
class CheckpointStoreTest {

    @TempDir
    Path tempDir;

    private CheckpointStore store;

    @BeforeEach
    void setUp() {
        store = new CheckpointStore(tempDir.toString(), new ObjectMapper());
    }

    // --- save and restore ---

    @Test
    void saveAndRestore_checkpoint() throws Exception {
        Map<String, Object> state = new HashMap<>();
        state.put("Ticker", "AAPL");
        state.put("ResearchPlan", "Analyze fundamentals");

        store.saveCheckpoint("AAPL", "2026-01-15", "researchPlan", state);

        var entry = store.getCheckpoint("AAPL", "2026-01-15");

        assertNotNull(entry);
        assertEquals("AAPL", entry.ticker());
        assertEquals("2026-01-15", entry.tradeDate());
        assertEquals("researchPlan", entry.lastCompletedPhase());
        assertTrue(entry.phases().containsKey("researchPlan"));
    }

    @Test
    void saveAndRestore_multiplePhases() throws Exception {
        store.saveCheckpoint("AAPL", "2026-01-15", "researchPlan",
                Map.of("plan", "Analyze fundamentals"));

        store.saveCheckpoint("AAPL", "2026-01-15", "debate",
                Map.of("briefs", "Debate complete"));

        var entry = store.getCheckpoint("AAPL", "2026-01-15");

        assertNotNull(entry);
        assertEquals("debate", entry.lastCompletedPhase());
        assertTrue(entry.phases().containsKey("researchPlan"));
        assertTrue(entry.phases().containsKey("debate"));
    }

    @Test
    void saveAndRestore_jsonSerializationDeserialization() throws Exception {
        Map<String, Object> state = new HashMap<>();
        state.put("Ticker", "NVDA");
        state.put("DebateBriefs", Map.of("bull", "Strong buy", "bear", "Hold"));
        state.put("InvestmentDebateState", Map.of("iterations", 3));

        store.saveCheckpoint("NVDA", "2026-01-15", "debate", state);

        var entry = store.getCheckpoint("NVDA", "2026-01-15");

        assertNotNull(entry);
        @SuppressWarnings("unchecked")
        Map<String, Object> phase = (Map<String, Object>) entry.phases().get("debate");
        assertNotNull(phase);
        @SuppressWarnings("unchecked")
        Map<String, Object> blackboard = (Map<String, Object>) phase.get("blackboard");
        assertNotNull(blackboard);
        assertEquals("NVDA", blackboard.get("Ticker"));
    }

    // --- hasCheckpoint / deleteCheckpoint ---

    @Test
    void hasCheckpoint_returnsFalseWhenNoFile() {
        assertFalse(store.hasCheckpoint("NVDA"));
    }

    @Test
    void hasCheckpoint_returnsTrueWhenFileExists() throws Exception {
        store.saveCheckpoint("AAPL", "2026-01-15", "researchPlan", Map.of("plan", "test"));

        assertTrue(store.hasCheckpoint("AAPL"));
    }

    @Test
    void deleteCheckpoint_removesFile() throws Exception {
        store.saveCheckpoint("AAPL", "2026-01-15", "researchPlan", Map.of("plan", "test"));
        assertTrue(store.hasCheckpoint("AAPL"));

        store.deleteCheckpoint("AAPL", "2026-01-15");

        assertFalse(store.hasCheckpoint("AAPL"));
    }

    @Test
    void deleteCheckpoint_noOpWhenNoFile() {
        assertDoesNotThrow(() -> store.deleteCheckpoint("NONEXISTENT", "2026-01-15"));
    }

    // --- atomic write ---

    @Test
    void saveCheckpoint_atomicWrite_tempFileRenamed() throws Exception {
        store.saveCheckpoint("AAPL", "2026-01-15", "researchPlan", Map.of("plan", "test"));

        // Check that the file exists and is valid JSON
        Path path = tempDir.resolve("AAPL.json");
        assertTrue(Files.exists(path));
        String content = Files.readString(path, StandardCharsets.UTF_8);
        assertTrue(content.contains("AAPL"));
        assertTrue(content.contains("researchPlan"));
    }

    @Test
    void saveCheckpoint_multipleWrites_atomic() throws Exception {
        for (int i = 0; i < 10; i++) {
            store.saveCheckpoint("AAPL", "2026-01-15", "phase" + i,
                    Map.of("iteration", i));
        }

        var entry = store.getCheckpoint("AAPL", "2026-01-15");
        assertNotNull(entry);
        assertEquals("phase9", entry.lastCompletedPhase());
        // All phases should be present
        assertEquals(10, entry.phases().size());
    }

    // --- path traversal protection ---

    @Test
    void checkpointPath_rejectsPathTraversal() {
        assertThrows(IllegalArgumentException.class, () ->
                store.saveCheckpoint("../etc", "2026-01-15", "test", Map.of()));
    }

    @Test
    void checkpointPath_rejectsSpecialCharacters() {
        assertThrows(IllegalArgumentException.class, () ->
                store.saveCheckpoint("A A P L", "2026-01-15", "test", Map.of()));
    }

    @Test
    void checkpointPath_acceptsValidTickers() throws Exception {
        // These should all work
        store.saveCheckpoint("AAPL", "2026-01-15", "test", Map.of());
        store.saveCheckpoint("MSFT", "2026-01-15", "test", Map.of());
        store.saveCheckpoint("GOOGL", "2026-01-15", "test", Map.of());
        store.saveCheckpoint("TSLA.US", "2026-01-15", "test", Map.of());

        assertTrue(store.hasCheckpoint("AAPL"));
        assertTrue(store.hasCheckpoint("MSFT"));
        assertTrue(store.hasCheckpoint("GOOGL"));
        assertTrue(store.hasCheckpoint("TSLA.US"));
    }

    // --- getCheckpoint returns null for missing ---

    @Test
    void getCheckpoint_returnsNullForMissingTicker() {
        var entry = store.getCheckpoint("NONEXISTENT", "2026-01-15");
        assertNull(entry);
    }
}
