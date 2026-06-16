package com.embabel.gekko.agent.memory;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A resolved trade decision with actual returns and LLM-generated reflection.
 */
public record ResolvedDecision(
        String ticker,
        String tradeDate,
        String rating,
        BigDecimal rawReturn,
        BigDecimal alphaReturn,
        String benchmark,
        int daysHeld,
        String reflection,
        LocalDateTime storedAt,
        LocalDateTime resolvedAt
) {
}
