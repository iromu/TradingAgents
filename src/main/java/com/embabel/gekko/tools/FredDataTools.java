package com.embabel.gekko.tools;

import com.embabel.gekko.dataflows.FredService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * LLM tool for fetching macroeconomic data from FRED.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FredDataTools {

    private final FredService fredService;

    @Value("${app.fred.enabled:true}")
    private boolean enabled;

    @Tool(name = "getMacroIndicators", description = "Fetch macroeconomic indicator data from FRED for a given series ID (e.g., GDP, CPIAUCSL, UNRATE). Returns a markdown table with date, value, change, and change percent.")
    public String getMacroIndicators(
            @ToolParam(description = "Economic indicator series ID (e.g., GDP, CPIAUCSL, UNRATE, FEDFUNDS, TB3MS)")
            String seriesId
    ) {
        if (!enabled) return "NO_DATA_AVAILABLE: FRED data is disabled (app.fred.enabled=false)";
        try {
            return fredService.getSeries(seriesId, 365);
        } catch (Exception e) {
            log.error("Error fetching FRED data for {}: {}", seriesId, e.getMessage());
            return "NO_DATA_AVAILABLE: Failed to fetch FRED data: " + e.getMessage();
        }
    }

    @Tool(name = "getMacroDashboard", description = "Fetch a dashboard of standard macroeconomic indicators (GDP, CPI, unemployment rate, Fed funds rate, 3-month T-bill). Returns markdown tables for each series.")
    public String getMacroDashboard(
            @ToolParam(description = "Comma-separated list of FRED series IDs (e.g., 'GDP,CPIAUCSL,UNRATE') or 'default' for standard dashboard")
            String seriesIds
    ) {
        if (!enabled) return "NO_DATA_AVAILABLE: FRED data is disabled (app.fred.enabled=false)";
        try {
            if (seriesIds == null || seriesIds.isBlank() || "default".equalsIgnoreCase(seriesIds)) {
                return fredService.getDashboard();
            }
            return fredService.getMultipleSeries(
                    Arrays.stream(seriesIds.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .toList()
            );
        } catch (Exception e) {
            log.error("Error fetching FRED dashboard: {}", e.getMessage());
            return "NO_DATA_AVAILABLE: Failed to fetch FRED dashboard: " + e.getMessage();
        }
    }
}
