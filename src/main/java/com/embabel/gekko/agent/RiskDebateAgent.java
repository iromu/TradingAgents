package com.embabel.gekko.agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.ActionContext;
import com.embabel.gekko.domain.ResearchTypes;
import com.embabel.gekko.util.FileCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.embabel.common.ai.model.ModelProvider.CHEAPEST_ROLE;

@Agent(description = "Risk Debate Agent — runs 3-round structured risk debate (bull → bear → judge)")
@Component
@RegisterReflectionForBinding({RiskAssessment.class, RiskLevel.class})
@RequiredArgsConstructor
@Slf4j
public class RiskDebateAgent {

    private static final int MAX_RISK_DEBATE_ROUNDS = 3;

    private final FileCache cache;

    @Action(description = "Assess risk via 3-round structured debate")
    @AchievesGoal(description = "Produce risk assessment")
    public RiskAssessment assessRisk(ResearchTypes.Ticker ticker, ResearchTypes.DebateBriefs briefs, ResearchTypes.InvestmentDebateState debateState, ActionContext actionContext) {
        List<String> riskyResponses = new ArrayList<>();
        List<String> neutralResponses = new ArrayList<>();
        List<String> conservativeResponses = new ArrayList<>();

        String prevSafe = "No response yet.";
        String prevNeutral = "No response yet.";

        for (int round = 0; round < MAX_RISK_DEBATE_ROUNDS; round++) {
            String riskyResponse = promptDebator(actionContext, "risk/AggressiveDebator", Map.of(
                    "trader_decision", "Invest",
                    "market_research_report", briefs.marketBrief(),
                    "sentiment_report", briefs.socialBrief(),
                    "news_report", briefs.newsBrief(),
                    "fundamentals_report", briefs.fundamentalsBrief(),
                    "history", debateState.history() != null ? String.join("\n", debateState.history()) : "",
                    "current_safe_response", prevSafe,
                    "current_neutral_response", prevNeutral
            ));
            riskyResponses.add(riskyResponse);

            String conservativeResponse = promptDebator(actionContext, "risk/ConservativeDebator", Map.of(
                    "trader_decision", "Invest",
                    "market_research_report", briefs.marketBrief(),
                    "sentiment_report", briefs.socialBrief(),
                    "news_report", briefs.newsBrief(),
                    "fundamentals_report", briefs.fundamentalsBrief(),
                    "history", debateState.history() != null ? String.join("\n", debateState.history()) : "",
                    "current_risky_response", riskyResponse,
                    "current_neutral_response", prevNeutral
            ));
            conservativeResponses.add(conservativeResponse);

            String neutralResponse = promptDebator(actionContext, "risk/NeutralDebator", Map.of(
                    "trader_decision", "Invest",
                    "market_research_report", briefs.marketBrief(),
                    "sentiment_report", briefs.socialBrief(),
                    "news_report", briefs.newsBrief(),
                    "fundamentals_report", briefs.fundamentalsBrief(),
                    "history", debateState.history() != null ? String.join("\n", debateState.history()) : "",
                    "current_risky_response", riskyResponse,
                    "current_safe_response", conservativeResponse
            ));
            neutralResponses.add(neutralResponse);

            log.info("Risk debate round {} complete: risky={}, neutral={}, conservative={}",
                    round + 1,
                    shortPreview(riskyResponse),
                    shortPreview(neutralResponse),
                    shortPreview(conservativeResponse));

            prevSafe = conservativeResponse;
            prevNeutral = neutralResponse;
        }

        String allResponses = String.join("\n\n--- RISKY ---\n\n", riskyResponses)
                + "\n\n--- CONSERVATIVE ---\n\n"
                + String.join("\n\n--- CONSERVATIVE ---\n\n", conservativeResponses)
                + "\n\n--- NEUTRAL ---\n\n"
                + String.join("\n\n--- NEUTRAL ---\n\n", neutralResponses);

        return judgeRisk(ticker.content(), allResponses, actionContext);
    }

    private String promptDebator(ActionContext actionContext, String templateName, Map<String, Object> model) {
        try {
            return actionContext.ai()
                    .withLlmByRole(CHEAPEST_ROLE)
                    .withId("riskDebator")
                    .withTemplate(templateName)
                    .createObject(String.class, model);
        } catch (Exception e) {
            log.warn("Risk debator prompt failed: {}", e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    private RiskAssessment judgeRisk(String ticker, String debateOutput, ActionContext actionContext) {
        String result = actionContext.ai()
                .withLlmByRole(CHEAPEST_ROLE)
                .withId("riskJudge")
                .withTemplate("managers/RiskManager")
                .createObject(String.class, Map.of(
                        "ticker", ticker.toUpperCase(),
                        "history", debateOutput,
                        "trader_decision", "Invest",
                        "past_memory_str", "No past memories found."
                ));

        return parseRiskAssessment(result);
    }

    private RiskAssessment parseRiskAssessment(String result) {
        if (result == null || result.isBlank()) {
            return new RiskAssessment(RiskLevel.NEUTRAL, "LLM returned empty result — defaulting to NEUTRAL");
        }

        String lower = result.toLowerCase();
        RiskLevel level;

        if (lower.contains("buy") && (lower.contains("risk") || lower.contains("bold") || lower.contains("aggressive") || lower.contains("high"))) {
            level = RiskLevel.RISKY;
        } else if (lower.contains("sell") || lower.contains("avoid") || lower.contains("cautious") || lower.contains("conservative") || lower.contains("safe")) {
            level = RiskLevel.CONSERVATIVE;
        } else {
            level = RiskLevel.NEUTRAL;
        }

        String reasoning = result;
        if (reasoning.length() > 200) {
            reasoning = reasoning.substring(0, 200) + "...";
        }

        return new RiskAssessment(level, reasoning);
    }

    private String shortPreview(String s) {
        if (s == null) return "(null)";
        return s.length() > 50 ? s.substring(0, 50) + "..." : s;
    }
}
