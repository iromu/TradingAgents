package com.embabel.gekko.agent.checkpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.embabel.agent.test.unit.FakeOperationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CheckpointAgent actions using FakeOperationContext.
 */
class CheckpointAgentTest {

    @TempDir
    Path tempDir;

    private CheckpointAgent agent;
    private CheckpointStore store;

    @BeforeEach
    void setUp() {
        store = new CheckpointStore(tempDir.toString(), new ObjectMapper());
        agent = new CheckpointAgent(store, new ObjectMapper(), true);
    }

    // --- restoreCheckpoint ---

    @Test
    void restoreCheckpoint_returnsNullWhenNoCheckpoint() {
        var result = agent.restoreCheckpoint("AAPL", "2026-01-15");
        assertNull(result);
    }

    @Test
    void restoreCheckpoint_restoresPhases() throws Exception {
        store.saveCheckpoint("AAPL", "2026-01-15", "researchPlan",
                Map.of("Ticker", "AAPL", "ResearchPlan", "Analyze fundamentals"));

        var result = agent.restoreCheckpoint("AAPL", "2026-01-15");

        assertNotNull(result);
        assertTrue(result.containsKey("researchPlan"));
        @SuppressWarnings("unchecked")
        Map<String, Object> phase = (Map<String, Object>) result.get("researchPlan");
        assertNotNull(phase);
        // The phase contains a "blackboard" key with the actual state
        @SuppressWarnings("unchecked")
        Map<String, Object> blackboard = (Map<String, Object>) phase.get("blackboard");
        assertNotNull(blackboard);
        assertEquals("AAPL", blackboard.get("Ticker"));
    }

    @Test
    void restoreCheckpoint_returnsNullWhenDisabled() {
        var disabledAgent = new CheckpointAgent(store, new ObjectMapper(), false);
        var result = disabledAgent.restoreCheckpoint("AAPL", "2026-01-15");
        assertNull(result);
    }

    // --- saveCheckpoint ---

    @Test
    void saveCheckpoint_savesBlackboardState() throws Exception {
        Map<String, Object> state = Map.of("Ticker", "AAPL", "Plan", "Buy");

        agent.saveCheckpoint("AAPL", "2026-01-15", "researchPlan", state);

        assertTrue(store.hasCheckpoint("AAPL"));
        var entry = store.getCheckpoint("AAPL", "2026-01-15");
        assertNotNull(entry);
        assertEquals("researchPlan", entry.lastCompletedPhase());
    }

    @Test
    void saveCheckpoint_skipsWhenDisabled() throws Exception {
        var disabledAgent = new CheckpointAgent(store, new ObjectMapper(), false);

        disabledAgent.saveCheckpoint("AAPL", "2026-01-15", "researchPlan",
                Map.of("Ticker", "AAPL"));

        assertFalse(store.hasCheckpoint("AAPL"));
    }

    @Test
    void saveCheckpoint_skipsEmptyState() throws Exception {
        agent.saveCheckpoint("AAPL", "2026-01-15", "researchPlan", Map.of());

        assertFalse(store.hasCheckpoint("AAPL"));
    }

    @Test
    void saveCheckpoint_skipsNullState() throws Exception {
        agent.saveCheckpoint("AAPL", "2026-01-15", "researchPlan", null);

        assertFalse(store.hasCheckpoint("AAPL"));
    }

    // --- clearCheckpoint ---

    @Test
    void clearCheckpoint_deletesFile() throws Exception {
        store.saveCheckpoint("AAPL", "2026-01-15", "researchPlan", Map.of("plan", "test"));
        assertTrue(store.hasCheckpoint("AAPL"));

        agent.clearCheckpoint("AAPL", "2026-01-15");

        assertFalse(store.hasCheckpoint("AAPL"));
    }

    @Test
    void clearCheckpoint_noOpWhenNoFile() {
        assertDoesNotThrow(() -> agent.clearCheckpoint("AAPL", "2026-01-15"));
    }

    @Test
    void clearCheckpoint_skipsWhenDisabled() throws Exception {
        store.saveCheckpoint("AAPL", "2026-01-15", "researchPlan", Map.of("plan", "test"));
        var disabledAgent = new CheckpointAgent(store, new ObjectMapper(), false);

        disabledAgent.clearCheckpoint("AAPL", "2026-01-15");

        assertTrue(store.hasCheckpoint("AAPL"));
    }
}
