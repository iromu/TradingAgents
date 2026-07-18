package com.embabel.gekko.agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.ActionContext;
import com.embabel.gekko.agent.risk.AggressiveDebator;
import com.embabel.gekko.agent.risk.ConservativeDebator;
import com.embabel.gekko.agent.risk.NeutralDebator;
import com.embabel.gekko.domain.ResearchTypes;
import com.embabel.gekko.util.AgentUtils;
import com.embabel.gekko.util.LlmBudgetTracker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;

import static com.embabel.common.ai.model.ModelProvider.BEST_ROLE;

/**
 * Risk Debate Agent — runs a 3-round structured risk debate with 3 debators
 * (Aggressive, Conservative, Neutral) in round-robin order, then judges the debate.
 * Mirrors Python's risk management team with round-robin turn-order via latest_speaker.
 */
@Agent(description = "Risk Debate Agent — runs 3-round structured risk debate (aggressive → conservative → neutral → judge)")
@Component
@RegisterReflectionForBinding({RiskAssessment.class, RiskAssessmentOutput.class, RiskLevel.class})
@RequiredArgsConstructor
@Slf4j
public class RiskDebateAgent {

    private static final int MAX_RISK_DEBATE_ROUNDS = 3;

    private final ObjectProvider<AggressiveDebator> aggressiveDebatorProvider;
    private final ObjectProvider<ConservativeDebator> conservativeDebatorProvider;
    private final ObjectProvider<NeutralDebator> neutralDebatorProvider;
    private final LlmBudgetTracker llmBudgetTracker;

    private AggressiveDebator getAggressiveDebator() {
        return aggressiveDebatorProvider.getObject();
    }

    private ConservativeDebator getConservativeDebator() {
        return conservativeDebatorProvider.getObject();
    }

    private NeutralDebator getNeutralDebator() {
        return neutralDebatorProvider.getObject();
    }

    private void trackCall(String ticker) {
        if (llmBudgetTracker != null) {
            llmBudgetTracker.recordCall(ticker);
        }
    }

    @Action(description = "Assess risk via 3-round structured debate with 3 debators")
    @AchievesGoal(description = "Produce risk assessment")
    public RiskAssessment assessRisk(
            ResearchTypes.Ticker ticker,
            ResearchTypes.DebateBriefs briefs,
            ResearchTypes.InvestmentDebateState debateState,
            String traderProposal,
            ActionContext actionContext
    ) {
        var aggressiveDebator = getAggressiveDebator();
        var conservativeDebator = getConservativeDebator();
        var neutralDebator = getNeutralDebator();

        List<String> aggressiveResponses = new ArrayList<>();
        List<String> conservativeResponses = new ArrayList<>();
        List<String> neutralResponses = new ArrayList<>();

        String currentAggressive = "";
        String currentConservative = "";
        String currentNeutral = "";

        StringBuilder history = new StringBuilder();
        if (debateState.history() != null) {
            history.append(String.join("\n", debateState.history()));
        }

        for (int round = 0; round < MAX_RISK_DEBATE_ROUNDS; round++) {
            // Aggressive speaks (Python: round-robin via latest_speaker, starts with Aggressive)
            trackCall(ticker.content());
            currentAggressive = aggressiveDebator.argue(
                    traderProposal,
                    briefs.marketBrief(),
                    briefs.socialBrief(),
                    briefs.newsBrief(),
                    briefs.fundamentalsBrief(),
                    history.toString(),
                    currentConservative,
                    currentNeutral,
                    actionContext
            );
            history.append("\n").append(currentAggressive);
            aggressiveResponses.add(currentAggressive);

            // Conservative speaks
            trackCall(ticker.content());
            currentConservative = conservativeDebator.argue(
                    traderProposal,
                    briefs.marketBrief(),
                    briefs.socialBrief(),
                    briefs.newsBrief(),
                    briefs.fundamentalsBrief(),
                    history.toString(),
                    currentAggressive,
                    currentNeutral,
                    actionContext
            );
            history.append("\n").append(currentConservative);
            conservativeResponses.add(currentConservative);

            // Neutral speaks
            trackCall(ticker.content());
            currentNeutral = neutralDebator.argue(
                    traderProposal,
                    briefs.marketBrief(),
                    briefs.socialBrief(),
                    briefs.newsBrief(),
                    briefs.fundamentalsBrief(),
                    history.toString(),
                    currentAggressive,
                    currentConservative,
                    actionContext
            );
            history.append("\n").append(currentNeutral);
            neutralResponses.add(currentNeutral);

            log.info("Risk debate round {} complete: aggressive={}, conservative={}, neutral={}",
                    round + 1,
                    shortPreview(currentAggressive),
                    shortPreview(currentConservative),
                    shortPreview(currentNeutral));
        }

        String allResponses = String.join("\n\n--- AGGRESSIVE ---\n\n", aggressiveResponses)
                + "\n\n--- CONSERVATIVE ---\n\n"
                + String.join("\n\n--- CONSERVATIVE ---\n\n", conservativeResponses)
                + "\n\n--- NEUTRAL ---\n\n"
                + String.join("\n\n--- NEUTRAL ---\n\n", neutralResponses);

        return judgeRisk(ticker.content(), allResponses, traderProposal, actionContext);
    }

    private RiskAssessment judgeRisk(String ticker, String debateOutput, String traderProposal, ActionContext actionContext) {
        trackCall(ticker);
        var model = Map.<String, Object>ofEntries(
                Map.entry("ticker", ticker.toUpperCase()),
                Map.entry("history", debateOutput),
                Map.entry("trader_decision", traderProposal),
                Map.entry("past_memory_str", AgentUtils.NO_PAST_MEMORY)
        );

        try {
            var output = actionContext.ai()
                    .withLlmByRole(BEST_ROLE)
                    .withId("riskJudge")
                    .withTemplate("managers/RiskManager")
                    .createObject(RiskAssessmentOutput.class, model);
            return new RiskAssessment(output.riskLevel(), output.reasoning());
        } catch (Exception e) {
            log.warn("Structured risk assessment failed, falling back to string parsing: {}", e.getMessage());
            var fallbackResult = actionContext.ai()
                    .withLlmByRole(BEST_ROLE)
                    .withId("riskJudge")
                    .withTemplate("managers/RiskManager")
                    .createObject(String.class, model);
            return parseRiskAssessmentFallback(fallbackResult);
        }
    }

    /**
     * Fallback parser for when structured output is unavailable.
     * Heuristic keyword matching on the LLM's free-text response.
     */
    private RiskAssessment parseRiskAssessmentFallback(String debateOutput) {
        if (debateOutput == null || debateOutput.isBlank()) {
            return new RiskAssessment(RiskLevel.NEUTRAL, "LLM returned empty result — defaulting to NEUTRAL");
        }

        var lower = debateOutput.toLowerCase();
        var level = classifyRisk(lower);
        var reasoning = truncate(debateOutput, 200);

        return new RiskAssessment(level, reasoning);
    }

    private RiskLevel classifyRisk(String lower) {
        if (lower.contains("buy") && riskWords(lower)) {
            return RiskLevel.RISKY;
        }
        if (sellWords(lower)) {
            return RiskLevel.CONSERVATIVE;
        }
        return RiskLevel.NEUTRAL;
    }

    private boolean riskWords(String s) {
        return s.contains("risk") || s.contains("bold") || s.contains("aggressive") || s.contains("high");
    }

    private boolean sellWords(String s) {
        return s.contains("sell") || s.contains("avoid") || s.contains("cautious")
                || s.contains("conservative") || s.contains("safe");
    }

    private String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }

    private String shortPreview(String s) {
        if (s == null) return "(null)";
        return s.length() > 50 ? s.substring(0, 50) + "..." : s;
    }
}
