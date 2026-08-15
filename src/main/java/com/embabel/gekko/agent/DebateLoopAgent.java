package com.embabel.gekko.agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.ActionContext;
import com.embabel.agent.api.common.workflow.loop.RepeatUntilBuilder;
import com.embabel.gekko.config.TraderAgentConfig;
import com.embabel.gekko.domain.ResearchTypes;
import com.embabel.gekko.agent.researchers.BearResearcher;
import com.embabel.gekko.agent.researchers.BullResearcher;
import com.embabel.gekko.util.FileCache;
import com.embabel.gekko.util.LlmBudgetTracker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Agent(description = "Debate Loop Agent — runs iterative bull/bear debate")
@Component
@RegisterReflectionForBinding({
        ResearchTypes.Ticker.class,
        ResearchTypes.DebateBriefs.class,
        ResearchTypes.InvestmentDebateState.class
})
@RequiredArgsConstructor
@Slf4j
public class DebateLoopAgent {

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private final BullResearcher bullResearcher;
    private final BearResearcher bearResearcher;
    private final FileCache cache;
    private final TraderAgentConfig config;
    private final LlmBudgetTracker llmBudgetTracker;

    @Action(description = "Run iterative bull/bear debate loop")
    @AchievesGoal(description = "Produce investment debate state")
    public ResearchTypes.InvestmentDebateState debate(ResearchTypes.Ticker ticker, ResearchTypes.DebateBriefs briefs, ActionContext actionContext) {
        if (briefs == null) {
            throw new IllegalArgumentException("briefs must not be null");
        }
        return RepeatUntilBuilder
                .returning(ResearchTypes.InvestmentDebateState.class)
                .withMaxIterations(config.maxDebateIterations())
                .repeating(ctx -> {
                    ResearchTypes.InvestmentDebateState last = ctx.lastAttempt();

                    List<String> history = last != null ? last.history() : new ArrayList<>();
                    List<String> bullHistory = last != null ? last.bullHistory() : new ArrayList<>();
                    List<String> bearHistory = last != null ? last.bearHistory() : new ArrayList<>();
                    int count = last != null ? last.count() : 0;

                    // Bull turn
                    String bullResponse = cache.getOrCompute(
                            "debate:" + ticker.content() + ":bull:" + count,
                            String.class,
                            () -> {
                                String result = bullResearcher.argue(briefs, history, actionContext);
                                llmBudgetTracker.recordCall(ticker.content());
                                return result;
                            }
                    );
                    history.add(bullResponse);
                    bullHistory.add(bullResponse);

                    // Bear turn
                    String bearResponse = cache.getOrCompute(
                            "debate:" + ticker.content() + ":bear:" + (count + 1),
                            String.class,
                            () -> {
                                String result = bearResearcher.argue(briefs, history, actionContext);
                                llmBudgetTracker.recordCall(ticker.content());
                                return result;
                            }
                    );
                    history.add(bearResponse);
                    bearHistory.add(bearResponse);

                    count += 2;

                    // Compute similarity once, reuse for logging and convergence check
                    double similarity = -1.0;
                    if (bullHistory.size() >= 2) {
                        String prevBull = bullHistory.get(bullHistory.size() - 2);
                        similarity = computeSimilarity(prevBull, bullResponse);
                        log.info("Debate iteration {} - bull similarity: {} (threshold: {})",
                                last.count() / 2, String.format("%.4f", similarity), String.format("%.4f", config.similarityThreshold()));
                    }

                    return new ResearchTypes.InvestmentDebateState(history, bullHistory, bearHistory, bearResponse, count, briefs, similarity);
                })
                .until(ctx -> {
                    ResearchTypes.InvestmentDebateState last = ctx.lastAttempt();
                    if (last == null) return false;
                    // Stop at max iterations
                    if (last.count() / 2 >= config.maxDebateIterations()) return true;
                    // Stop on convergence: reuse cached similarity from repeating block
                    if (last.lastSimilarity() >= config.similarityThreshold()) {
                        log.info("Debate converged at iteration {} (similarity: {} >= {})",
                                last.count() / 2, String.format("%.4f", last.lastSimilarity()), String.format("%.4f", config.similarityThreshold()));
                        return true;
                    }
                    return false;
                })
                .build()
                .asSubProcess(actionContext, ResearchTypes.InvestmentDebateState.class);
    }

    /**
     * Compute Jaccard similarity between two strings using bigrams.
     * Returns a value between 0.0 (no overlap) and 1.0 (identical).
     */
    private double computeSimilarity(String a, String b) {
        if (a == null || a.isBlank()) {
            return b == null || b.isBlank() ? 1.0 : 0.0;
        }
        if (b == null || b.isBlank()) {
            return 0.0;
        }
        Set<String> bigramsA = bigrams(a);
        Set<String> bigramsB = bigrams(b);
        if (bigramsA.isEmpty() && bigramsB.isEmpty()) return 1.0;
        if (bigramsA.isEmpty() || bigramsB.isEmpty()) return 0.0;

        Set<String> intersection = new HashSet<>(bigramsA);
        intersection.retainAll(bigramsB);

        Set<String> union = new HashSet<>(bigramsA);
        union.addAll(bigramsB);

        return (double) intersection.size() / union.size();
    }

    private Set<String> bigrams(String text) {
        Set<String> result = new HashSet<>();
        String normalized = WHITESPACE.matcher(text.toLowerCase()).replaceAll(" ").trim();
        for (int i = 0; i < normalized.length() - 1; i++) {
            result.add(normalized.substring(i, i + 2));
        }
        return result;
    }
}
