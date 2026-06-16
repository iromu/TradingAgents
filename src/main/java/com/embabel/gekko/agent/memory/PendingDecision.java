package com.embabel.gekko.agent.memory;

import java.time.LocalDateTime;

/**
 * A pending trade decision awaiting resolution with actual returns.
 */
public record PendingDecision(
        String ticker,
        String tradeDate,
        String rating,
        String executiveSummary,
        String investmentThesis,
        LocalDateTime storedAt
) {
}
