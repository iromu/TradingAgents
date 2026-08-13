package com.embabel.gekko.agent.memory;

import com.embabel.agent.api.common.OperationContext;
import com.embabel.gekko.dataflows.YFinService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.embabel.common.ai.model.ModelProvider.BEST_ROLE;

/**
 * Decision memory system that learns from past outcomes.
 * Stores decisions, resolves them with actual returns, and injects past context.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DecisionMemoryAgent {

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final DecisionMemoryRepository repository;
    private final YFinService yFinService;

    public String storeDecision(String ticker, String tradeDate, String rating,
                                String executiveSummary, String investmentThesis) {
        repository.appendPending(ticker, tradeDate, rating, executiveSummary, investmentThesis);
        log.info("Stored pending decision for {} on {}", ticker, tradeDate);
        return "Decision stored for " + ticker;
    }

    public String storeAndResolveWithReflection(String ticker, String tradeDate, String rating,
                                                String executiveSummary, String investmentThesis,
                                                OperationContext context) {
        storeDecision(ticker, tradeDate, rating, executiveSummary, investmentThesis);
        resolvePending(ticker, tradeDate, context);
        return "Stored and resolved decision for " + ticker;
    }

    public void resolvePending(String ticker, String tradeDate, OperationContext context) {
        if (!repository.hasPendingEntriesFor(ticker)) {
            log.debug("No pending entries for {}, skipping resolution", ticker);
            return;
        }

        var pendingEntries = repository.getPendingEntries(ticker);
        for (var pending : pendingEntries) {
            if (!pending.tradeDate().equals(tradeDate)) continue;

            try {
                // Fetch actual returns
                var returns = fetchReturns(ticker, tradeDate);

                // LLM reflection using BEST_ROLE model
                String reflection;
                if (context != null) {
                    reflection = generateReflection(context, pending, returns);
                } else {
                    reflection = "Return was " + returns.rawReturn() + "% for " + pending.ticker();
                }

                // Atomic update
                repository.resolve(ticker, tradeDate, returns.rawReturn, returns.alphaReturn,
                        returns.benchmark, returns.daysHeld, reflection);

                log.info("Resolved decision for {} on {} with reflection", ticker, tradeDate);
            } catch (Exception e) {
                log.error("Failed to resolve decision for {} on {}: {}", ticker, tradeDate, e.getMessage());
            }
        }
    }

    public String generatePastContext(String ticker) {
        return repository.generatePastContext(ticker);
    }

    public ReturnsData fetchReturns(String ticker, String tradeDate) throws Exception {
        // Fetch 5-day return from Yahoo Finance
        LocalDate trade = LocalDate.parse(tradeDate, DF);
        LocalDate end = trade.plusDays(5);

        String data = yFinService.getYFinDataOnline(ticker, tradeDate, end.format(DF));

        // Parse the CSV data to get close prices
        String[] lines = data.split("\n");

        // Parse header row and validate expected columns by name
        Map<String, Integer> columns = parseCsvHeader(lines);
        int dateIdx = resolveColumn(columns, "Date");
        int openIdx = resolveColumn(columns, "Open");
        int closeIdx = resolveColumn(columns, "Close");

        double openPrice = 0;
        double closePrice = 0;

        for (String line : lines) {
            if (line.startsWith("#") || line.isBlank()) continue;

            String[] parts = line.split(",");
            int maxIdx = Math.max(dateIdx, Math.max(openIdx, closeIdx));
            if (parts.length <= maxIdx) continue;

            try {
                LocalDate lineDate = LocalDate.parse(parts[dateIdx], DF);
                if (lineDate.equals(trade)) {
                    openPrice = parseFiniteDouble(parts[openIdx]);
                }
                if (lineDate.equals(end)) {
                    closePrice = parseFiniteDouble(parts[closeIdx]);
                }
            } catch (Exception e) {
                log.debug("Skipping malformed CSV line: {}", line);
                continue;
            }
        }

        if (openPrice == 0 || closePrice == 0) {
            return new ReturnsData(BigDecimal.ZERO, BigDecimal.ZERO, "SPY", 5);
        }

        BigDecimal rawReturn = BigDecimal.valueOf((closePrice - openPrice) / openPrice * 100);
        // Alpha vs benchmark (SPY) — simplified: assume benchmark return is 0 for now
        BigDecimal alphaReturn = rawReturn;

        return new ReturnsData(rawReturn, alphaReturn, "SPY", 5);
    }

    private Map<String, Integer> parseCsvHeader(String[] lines) {
        for (String line : lines) {
            if (line.startsWith("#") || line.isBlank()) continue;
            String[] parts = line.split(",");
            Map<String, Integer> columns = new LinkedHashMap<>();
            for (int i = 0; i < parts.length; i++) {
                columns.put(parts[i].trim(), i);
            }
            return columns;
        }
        return Map.of();
    }

    private int resolveColumn(Map<String, Integer> columns, String name) {
        Integer idx = columns.get(name);
        if (idx == null) {
            throw new IllegalArgumentException("CSV missing required column: " + name
                    + ". Found: " + columns.keySet());
        }
        return idx;
    }

    private double parseFiniteDouble(String raw) {
        double value = Double.parseDouble(raw);
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Non-finite value in CSV: " + raw);
        }
        return value;
    }

    private String generateReflection(OperationContext context, PendingDecision pending, ReturnsData returns) {
        var model = java.util.Map.<String, Object>of(
                "rating", pending.rating(),
                "ticker", pending.ticker(),
                "trade_date", pending.tradeDate(),
                "raw_return", returns.rawReturn(),
                "alpha_return", returns.alphaReturn()
        );

        try {
            return context.ai()
                    .withLlmByRole(BEST_ROLE)
                    .withId("memory-reflection")
                    .creating(String.class)
                    .fromTemplate("memory/reflection", model);
        } catch (Exception e) {
            log.warn("Failed to generate reflection: {}", e.getMessage());
            return "Return was " + returns.rawReturn() + "% for " + pending.ticker();
        }
    }

    /**
     * Holds actual returns data for a resolved decision.
     */
    public record ReturnsData(
            BigDecimal rawReturn,
            BigDecimal alphaReturn,
            String benchmark,
            int daysHeld
    ) {
    }
}
