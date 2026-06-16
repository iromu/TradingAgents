package com.embabel.gekko.agent.memory;

import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.OperationContext;
import com.embabel.gekko.dataflows.YFinService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static com.embabel.common.ai.model.ModelProvider.BEST_ROLE;

/**
 * Decision memory system that learns from past outcomes.
 * Stores decisions, resolves them with actual returns, and injects past context.
 */
@Agent(description = "Decision memory system that learns from past trading outcomes")
@Component
@RequiredArgsConstructor
@Slf4j
public class DecisionMemoryAgent {

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final DecisionMemoryRepository repository;
    private final YFinService yFinService;

    @Action(description = "Store a pending decision for later resolution with actual returns")
    public void storeDecision(String ticker, String tradeDate, String rating,
                              String executiveSummary, String investmentThesis) {
        repository.appendPending(ticker, tradeDate, rating, executiveSummary, investmentThesis);
        log.info("Stored pending decision for {} on {}", ticker, tradeDate);
    }

    @Action(description = "Resolve pending decisions with actual returns and generate LLM reflection")
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

    @Action(description = "Generate past_context for injection into Portfolio Manager prompt")
    public String generatePastContext(String ticker) {
        return repository.generatePastContext(ticker);
    }

    @Action(description = "Fetch actual returns for a ticker on a given date")
    public ReturnsData fetchReturns(String ticker, String tradeDate) throws Exception {
        // Fetch 5-day return from Yahoo Finance
        LocalDate trade = LocalDate.parse(tradeDate, DF);
        LocalDate end = trade.plusDays(5);

        String data = yFinService.getYFinDataOnline(ticker, tradeDate, end.format(DF));

        // Parse the CSV data to get close prices
        String[] lines = data.split("\n");
        double openPrice = 0;
        double closePrice = 0;

        for (String line : lines) {
            if (line.startsWith("#") || line.startsWith("Date,")) continue;
            String[] parts = line.split(",");
            if (parts.length < 7) continue;

            LocalDate lineDate = LocalDate.parse(parts[0], DF);
            if (lineDate.equals(trade)) {
                openPrice = Double.parseDouble(parts[1]);
            }
            if (lineDate.equals(end)) {
                closePrice = Double.parseDouble(parts[4]); // Close price
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
                    .withTemplate("memory/reflection")
                    .createObject(String.class, model);
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
