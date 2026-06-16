package com.embabel.gekko.tools;

import com.embabel.gekko.dataflows.PolymarketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * LLM tool for fetching prediction market data from Polymarket.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PolymarketDataTools {

    private final PolymarketService polymarketService;

    @Value("${app.polymarket.enabled:true}")
    private boolean enabled;

    @Tool(name = "getPredictionMarkets", description = "Search for prediction market outcomes and probabilities on Polymarket. Returns a markdown table with market names, outcomes, and probabilities.")
    public String getPredictionMarkets(
            @ToolParam(description = "Market topic or question to search (e.g., 'Fed rate cut', 'election', 'inflation')")
            String query
    ) {
        if (!enabled) return "NO_DATA_AVAILABLE: Polymarket data is disabled (app.polymarket.enabled=false)";
        try {
            return polymarketService.searchMarkets(query);
        } catch (Exception e) {
            log.error("Error fetching Polymarket data for '{}': {}", query, e.getMessage());
            return "NO_DATA_AVAILABLE: Failed to fetch Polymarket data: " + e.getMessage();
        }
    }
}
