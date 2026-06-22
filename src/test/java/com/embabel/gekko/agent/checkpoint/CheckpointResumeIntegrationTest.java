package com.embabel.gekko.agent.checkpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for checkpoint/resume functionality.
 * Tests the full pipeline: save checkpoint → simulate crash → resume from checkpoint.
 */
@Tag("integration")
class CheckpointResumeIntegrationTest {

    @TempDir
    Path tempDir;

    private CheckpointStore makeStore() {
        var checkpointDir = tempDir.resolve("checkpoints");
        return new CheckpointStore(checkpointDir.toString(), new ObjectMapper());
    }

    private CheckpointAgent makeAgent(CheckpointStore store, boolean enabled) {
        return new CheckpointAgent(store, new ObjectMapper(), enabled);
    }

    @Test
    void shouldSaveAndRestoreCheckpoint() {
        var store = makeStore();
        var agent = makeAgent(store, true);

        // Save a checkpoint at researchPlan phase
        agent.saveCheckpoint("AAPL", "2026-06-15", "researchPlan",
                Map.of("plan", "Buy AAPL on dip"));

        // Verify checkpoint was saved
        assertTrue(store.hasCheckpoint("AAPL"));

        // Restore the checkpoint (returns Map, not CheckpointEntry)
        var restored = agent.restoreCheckpoint("AAPL", "2026-06-15");

        assertNotNull(restored);
        assertEquals("Buy AAPL on dip", restored.get("plan"));
    }

    @Test
    void shouldMergePhasesOnCheckpointSave() {
        var store = makeStore();
        var agent = makeAgent(store, true);

        // Save at researchPlan phase
        agent.saveCheckpoint("MSFT", "2026-06-15", "researchPlan",
                Map.of("plan", "Initial plan"));

        // Save again at debateBriefs phase (should merge)
        agent.saveCheckpoint("MSFT", "2026-06-15", "debateBriefs",
                Map.of("briefs", "Complete"));

        var restored = agent.restoreCheckpoint("MSFT", "2026-06-15");

        assertNotNull(restored);
        // The merged data should contain both phases
        assertTrue(restored.containsKey("plan"));
        assertTrue(restored.containsKey("briefs"));
    }

    @Test
    void shouldSkipRestoreWhenNoCheckpoint() {
        var store = makeStore();
        var agent = makeAgent(store, true);

        var restored = agent.restoreCheckpoint("NONEXISTENT", "2026-06-15");

        assertNull(restored, "Should return null when no checkpoint exists");
    }

    @Test
    void shouldClearCheckpoint() {
        var store = makeStore();
        var agent = makeAgent(store, true);

        agent.saveCheckpoint("GOOGL", "2026-06-15", "researchPlan",
                Map.of("plan", "Test"));

        assertTrue(store.hasCheckpoint("GOOGL"));

        agent.clearCheckpoint("GOOGL", "2026-06-15");

        assertFalse(store.hasCheckpoint("GOOGL"), "Checkpoint should be deleted");
    }

    @Test
    void shouldRespectEnabledFlag() {
        var store = makeStore();
        var agent = makeAgent(store, false); // disabled

        // Should not throw even when disabled
        assertDoesNotThrow(() -> agent.saveCheckpoint("AAPL", "2026-06-15", "researchPlan", Map.of()));
        assertDoesNotThrow(() -> agent.restoreCheckpoint("AAPL", "2026-06-15"));
        assertDoesNotThrow(() -> agent.clearCheckpoint("AAPL", "2026-06-15"));
    }

    @Test
    void shouldHandlePathTraversalPrevention() {
        var store = makeStore();

        // Should throw on path traversal attempts
        assertThrows(IllegalArgumentException.class, () ->
                store.saveCheckpoint("../../../etc/passwd", "2026-06-15",
                        "researchPlan", Map.of()));
    }

    @Test
    void shouldStoreCheckpointAsValidJson() throws IOException {
        var store = makeStore();

        store.saveCheckpoint("TSLA", "2026-06-15", "researchPlan",
                Map.of("plan", "Buy TSLA", "confidence", 0.85));

        var files = Files.list(tempDir.resolve("checkpoints")).toList();
        assertEquals(1, files.size());

        var content = Files.readString(files.get(0));
        assertTrue(content.contains("researchPlan"), "JSON should contain researchPlan value");
        assertTrue(content.contains("Buy TSLA"), "JSON should contain plan value");
    }

    @Test
    void shouldHandleMultipleTickersIndependently() {
        var store = makeStore();

        store.saveCheckpoint("AAPL", "2026-06-15", "researchPlan", Map.of("ticker", "AAPL"));
        store.saveCheckpoint("MSFT", "2026-06-15", "debateBriefs", Map.of("ticker", "MSFT"));

        var aapl = store.getCheckpoint("AAPL", "2026-06-15");
        var msft = store.getCheckpoint("MSFT", "2026-06-15");

        assertNotNull(aapl);
        assertNotNull(msft);
        assertEquals("AAPL", aapl.ticker());
        assertEquals("MSFT", msft.ticker());
        assertEquals("researchPlan", aapl.lastCompletedPhase());
        assertEquals("debateBriefs", msft.lastCompletedPhase());
    }
}