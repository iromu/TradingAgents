package com.embabel.gekko.agent.checkpoint;

import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Checkpoint manager for crash recovery.
 * Saves blackboard snapshots after each phase and restores on restart.
 */
@Agent(description = "Checkpoint manager for crash recovery via blackboard snapshot persistence")
@Component
@Slf4j
public class CheckpointAgent {

    private final CheckpointStore store;
    private final ObjectMapper mapper;
    private final boolean checkpointEnabled;

    public CheckpointAgent(CheckpointStore store, ObjectMapper mapper,
                           @Value("${app.checkpoint.enabled:false}") boolean checkpointEnabled) {
        this.store = store;
        this.mapper = mapper;
        this.checkpointEnabled = checkpointEnabled;
    }

    @Action(description = "Restore blackboard from checkpoint if exists for the given ticker/date")
    public Map<String, Object> restoreCheckpoint(String ticker, String tradeDate) {
        if (!checkpointEnabled) {
            log.debug("Checkpoints disabled, skipping restore");
            return null;
        }
        try {
            var checkpoint = store.getCheckpoint(ticker, tradeDate);
            if (checkpoint != null) {
                log.info("Restored checkpoint for {} on {} (phase: {})",
                        ticker, tradeDate, checkpoint.lastCompletedPhase());
                return checkpoint.phases();
            }
            log.debug("No checkpoint found for {} on {}", ticker, tradeDate);
            return null;
        } catch (Exception e) {
            log.error("Failed to restore checkpoint for {}: {}", ticker, e.getMessage());
            return null;
        }
    }

    @Action(description = "Save blackboard snapshot after each phase completes")
    public void saveCheckpoint(String ticker, String tradeDate, String phase,
                               Map<String, Object> blackboardState) {
        if (!checkpointEnabled) return;
        if (blackboardState == null || blackboardState.isEmpty()) {
            log.debug("No blackboard state to save for {} phase {}", ticker, phase);
            return;
        }
        try {
            store.saveCheckpoint(ticker, tradeDate, phase, blackboardState);
            log.info("Saved checkpoint for {} on {} (phase: {})", ticker, tradeDate, phase);
        } catch (Exception e) {
            log.error("Failed to save checkpoint for {} phase {}: {}", ticker, phase, e.getMessage());
        }
    }

    @Action(description = "Clear checkpoint on successful completion")
    public void clearCheckpoint(String ticker, String tradeDate) {
        if (!checkpointEnabled) return;
        try {
            store.deleteCheckpoint(ticker);
            log.info("Cleared checkpoint for {}", ticker);
        } catch (Exception e) {
            log.error("Failed to clear checkpoint for {}: {}", ticker, e.getMessage());
        }
    }
}
