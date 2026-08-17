package com.embabel.gekko.agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.ActionContext;
import com.embabel.gekko.agent.risk.AggressiveDebator;
import com.embabel.gekko.agent.risk.ConservativeDebator;
import com.embabel.gekko.agent.risk.NeutralDebator;
import com.embabel.gekko.config.TraderAgentConfig;
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
import java.util.Objects;

import org.springframework.beans.factory.ObjectProvider;

import static com.embabel.common.ai.model.ModelProvider.BEST_ROLE;

/**
 * Risk Debate Agent — 3-round structured risk debate (aggressive → conservative → neutral → judge).
 * Mirrors Python's round-robin via latest_speaker pattern.
 */
@Agent(description = "Risk Debate Agent — runs 3-round structured risk debate (aggressive → conservative → neutral → judge)")
@Component
@RegisterReflectionForBinding({RiskAssessment.class, RiskAssessmentOutput.class, RiskLevel.class})
@RequiredArgsConstructor
@Slf4j
public class RiskDebateAgent {

    private final TraderAgentConfig config;
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
        Objects.requireNonNull(briefs, "briefs must not be null");
        Objects.requireNonNull(debateState, "debateState must not be null");
        Objects.requireNonNull(traderProposal, "traderProposal must not be null");

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

        for (int round = 0; round < config.maxRiskDebateRounds(); round++) {
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

        StringBuilder allResponses = new StringBuilder();
        for (int i = 0; i < aggressiveResponses.size(); i++) {
            if (i > 0) allResponses.append("\n\n");
            allResponses.append("Aggressive (Round ").append(i + 1).append("):\n")
                    .append(aggressiveResponses.get(i));
        }
        for (int i = 0; i < conservativeResponses.size(); i++) {
            if (allResponses.length() > 0) allResponses.append("\n\n");
            allResponses.append("Conservative (Round ").append(i + 1).append("):\n")
                    .append(conservativeResponses.get(i));
        }
        for (int i = 0; i < neutralResponses.size(); i++) {
            if (allResponses.length() > 0) allResponses.append("\n\n");
            allResponses.append("Neutral (Round ").append(i + 1).append("):\n")
                    .append(neutralResponses.get(i));
        }

        return judgeRisk(ticker.content(), allResponses.toString(), traderProposal, actionContext);
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
                    .creating(RiskAssessmentOutput.class)
                    .fromTemplate("managers/RiskManager", model);
            return new RiskAssessment(output.riskLevel(), output.reasoning());
        } catch (Exception e) {
            log.warn("Structured risk assessment failed, falling back to string parsing: {}", e.getMessage());
            var fallbackResult = actionContext.ai()
                    .withLlmByRole(BEST_ROLE)
                    .withId("riskJudge")
                    .creating(String.class)
                    .fromTemplate("managers/RiskManager", model);
            return parseRiskAssessmentFallback(fallbackResult);
        }
    }

    /**
     * Fallback parser for when structured output is unavailable.
     * Returns NEUTRAL with an explicit undetermined marker — never classifies
     * RISKY or CONSERVATIVE from keyword matching on the transcript, because
     * speaker labels ("Aggressive (Round 1)", "Conservative (Round 2)") always
     * contain those keywords and would produce a false positive.
     */
    private RiskAssessment parseRiskAssessmentFallback(String debateOutput) {
        if (debateOutput == null || debateOutput.isBlank()) {
            return new RiskAssessment(RiskLevel.NEUTRAL, "LLM returned empty result — defaulting to NEUTRAL");
        }
        String reasoning = truncate(debateOutput, 200);
        return new RiskAssessment(RiskLevel.NEUTRAL,
                "Structured output unavailable — undetermined, defaulting to NEUTRAL. Judge said: " + reasoning);
    }

    private String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }

    private String shortPreview(String s) {
        if (s == null) return "(null)";
        return s.length() > 50 ? s.substring(0, 50) + "..." : s;
    }
}
