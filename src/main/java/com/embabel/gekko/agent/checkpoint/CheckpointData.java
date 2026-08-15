package com.embabel.gekko.agent.checkpoint;

import java.util.Map;

/**
 * Data returned when restoring a checkpoint.
 * Contains the ticker, trade date, last completed phase, and phase blackboard states.
 */
public record CheckpointData(
        String ticker,
        String tradeDate,
        String lastCompletedPhase,
        Map<String, Object> phases
) {
}
