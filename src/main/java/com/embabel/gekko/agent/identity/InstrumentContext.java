package com.embabel.gekko.agent.identity;

/**
 * Resolved company identity for a ticker symbol.
 * Used to prevent LLM hallucination by anchoring agents to real company metadata.
 */
public record InstrumentContext(
        String ticker,
        String companyName,
        String sector,
        String industry,
        String exchange,
        String currency
) {
}
