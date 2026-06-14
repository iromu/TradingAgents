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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Agent(description = "Debate Loop Agent — runs iterative bull/bear debate")
@Component
@RequiredArgsConstructor
@Slf4j
public class DebateLoopAgent {

    private final BullResearcher bullResearcher;
    private final BearResearcher bearResearcher;
    private final FileCache cache;
    private final TraderAgentConfig config;

    @Action(description = "Run iterative bull/bear debate loop")
    @AchievesGoal(description = "Produce investment debate state")
    public ResearchTypes.InvestmentDebateState debate(ResearchTypes.Ticker ticker, ResearchTypes.DebateBriefs briefs, ActionContext actionContext) {
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
                            ticker.content() + "_debate_" + count + "_bull",
                            String.class,
                            () -> bullResearcher.argue(briefs, history, actionContext)
                    );
                    history.add(bullResponse);
                    bullHistory.add(bullResponse);

                    // Bear turn
                    String bearResponse = cache.getOrCompute(
                            ticker.content() + "_debate_" + (count + 1) + "_bear",
                            String.class,
                            () -> bearResearcher.argue(briefs, history, actionContext)
                    );
                    history.add(bearResponse);
                    bearHistory.add(bearResponse);

                    count += 2;

                    // Log similarity for debugging
                    if (bullHistory.size() >= 2) {
                        String prevBull = bullHistory.get(bullHistory.size() - 2);
                        double similarity = computeSimilarity(prevBull, bullResponse);
                        log.info("Debate iteration {} - bull similarity: {:.4f} (threshold: {:.4f})",
                                count, similarity, config.similarityThreshold());
                    }

                    return new ResearchTypes.InvestmentDebateState(history, bullHistory, bearHistory, bearResponse, count, briefs, null);
                })
                .until(ctx -> {
                    ResearchTypes.InvestmentDebateState last = ctx.lastAttempt();
                    if (last == null) return false;
                    // Stop at max iterations
                    if (last.count() >= config.maxDebateIterations()) return true;
                    // Stop on convergence: check if last two bull responses are similar enough
                    if (last.bullHistory().size() >= 2) {
                        String prevBull = last.bullHistory().get(last.bullHistory().size() - 2);
                        String currBull = last.bullHistory().get(last.bullHistory().size() - 1);
                        double similarity = computeSimilarity(prevBull, currBull);
                        if (similarity >= config.similarityThreshold()) {
                            log.info("Debate converged at iteration {} (similarity: {:.4f} >= {:.4f})",
                                    last.count(), similarity, config.similarityThreshold());
                            return true;
                        }
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
        String normalized = text.toLowerCase().replaceAll("\\s+", " ").trim();
        for (int i = 0; i < normalized.length() - 1; i++) {
            result.add(normalized.substring(i, i + 2));
        }
        return result;
    }
}
