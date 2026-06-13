package com.embabel.gekko.agent;

import com.embabel.agent.api.common.ActionContext;
import com.embabel.gekko.agent.TraderAgent.DebateBriefs;
import com.embabel.gekko.agent.TraderAgent.InvestmentDebateState;
import com.embabel.gekko.agent.TraderAgent.Ticker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.embabel.common.ai.model.ModelProvider.CHEAPEST_ROLE;

/**
 * Service that runs the risk debate between Aggressive, Conservative, and Neutral debaters.
 * Produces a RiskAssessment with level and reasoning.
 */
@Component
@Slf4j
public class RiskDebateService {

    @Value("classpath:prompts/managers/RiskManager.jinja")
    private Resource promptRiskManager;

    @Value("classpath:prompts/risk/AggressiveDebator.jinja")
    private Resource promptAggressiveDebator;

    @Value("classpath:prompts/risk/ConservativeDebator.jinja")
    private Resource promptConservativeDebator;

    @Value("classpath:prompts/risk/NeutralDebator.jinja")
    private Resource promptNeutralDebator;

    private static final int MAX_RISK_DEBATE_ROUNDS = 3;

    /**
     * Run the risk debate from the full pipeline context.
     *
     * @param ticker the ticker being analyzed
     * @param briefs the debate briefs from analyst reports
     * @param debateState the investment debate state
     * @param actionContext the action context for LLM calls
     * @return the risk assessment result
     */
    public RiskAssessment runRiskDebate(
            Ticker ticker,
            DebateBriefs briefs,
            InvestmentDebateState debateState,
            ActionContext actionContext
    ) {
        return runMultiAgentRiskDebate(
                ticker.content(),
                briefs.fundamentalsBrief(),
                briefs.marketBrief(),
                briefs.newsBrief(),
                briefs.socialBrief(),
                debateState.history() != null ? String.join("\n", debateState.history()) : "",
                actionContext
        );
    }

    /**
     * Assess the risk level of an investment based on all analyst reports and debate history.
     *
     * @param ticker the ticker symbol
     * @param fundamentalsReport fundamentals report content
     * @param marketReport market report content
     * @param newsReport news report content
     * @param socialReport social media report content
     * @param debateHistory the investment debate history
     * @param actionContext the action context for LLM calls
     * @return the risk assessment result
     */
    public RiskAssessment assessRisk(
            String ticker,
            String fundamentalsReport,
            String marketReport,
            String newsReport,
            String socialReport,
            String debateHistory,
            ActionContext actionContext
    ) {
        return runMultiAgentRiskDebate(
                ticker, fundamentalsReport, marketReport, newsReport, socialReport, debateHistory, actionContext
        );
    }

    /**
     * Run a multi-agent risk debate between Risky, Neutral, and Conservative debaters.
     * Each debater responds to the others' arguments, then the RiskManager makes a final decision.
     */
    private RiskAssessment runMultiAgentRiskDebate(
            String ticker,
            String fundamentalsReport,
            String marketReport,
            String newsReport,
            String socialReport,
            String debateHistory,
            ActionContext actionContext
    ) {
        List<String> riskyResponses = new ArrayList<>();
        List<String> neutralResponses = new ArrayList<>();
        List<String> conservativeResponses = new ArrayList<>();

        String prevSafe = "No response yet.";
        String prevNeutral = "No response yet.";

        for (int round = 0; round < MAX_RISK_DEBATE_ROUNDS; round++) {
            // Risky debater responds to conservative and neutral
            String riskyResponse = promptDebator(
                    actionContext, "risk/AggressiveDebator",
                    Map.of(
                            "trader_decision", "Invest",
                            "market_research_report", marketReport,
                            "sentiment_report", socialReport,
                            "news_report", newsReport,
                            "fundamentals_report", fundamentalsReport,
                            "history", debateHistory,
                            "current_safe_response", prevSafe,
                            "current_neutral_response", prevNeutral
                    )
            );
            riskyResponses.add(riskyResponse);

            // Conservative debater responds to risky and neutral
            String conservativeResponse = promptDebator(
                    actionContext, "risk/ConservativeDebator",
                    Map.of(
                            "trader_decision", "Invest",
                            "market_research_report", marketReport,
                            "sentiment_report", socialReport,
                            "news_report", newsReport,
                            "fundamentals_report", fundamentalsReport,
                            "history", debateHistory,
                            "current_risky_response", riskyResponse,
                            "current_neutral_response", prevNeutral
                    )
            );
            conservativeResponses.add(conservativeResponse);

            // Neutral debater responds to risky and conservative
            String neutralResponse = promptDebator(
                    actionContext, "risk/NeutralDebator",
                    Map.of(
                            "trader_decision", "Invest",
                            "market_research_report", marketReport,
                            "sentiment_report", socialReport,
                            "news_report", newsReport,
                            "fundamentals_report", fundamentalsReport,
                            "history", debateHistory,
                            "current_risky_response", riskyResponse,
                            "current_safe_response", conservativeResponse
                    )
            );
            neutralResponses.add(neutralResponse);

            String safePreview = conservativeResponse.length() > 50 ? conservativeResponse.substring(0, 50) + "..." : conservativeResponse;
            String neutralPreview = neutralResponse.length() > 50 ? neutralResponse.substring(0, 50) + "..." : neutralResponse;
            String riskyPreview = riskyResponse.length() > 50 ? riskyResponse.substring(0, 50) + "..." : riskyResponse;

            log.info("Risk debate round {} complete: risky={}, neutral={}, conservative={}",
                    round + 1, riskyPreview, neutralPreview, safePreview);

            prevSafe = conservativeResponse;
            prevNeutral = neutralResponse;
        }

        String allResponses = String.join("\n\n--- RISKY ---\n\n", riskyResponses)
                + "\n\n--- CONSERVATIVE ---\n\n"
                + String.join("\n\n--- CONSERVATIVE ---\n\n", conservativeResponses)
                + "\n\n--- NEUTRAL ---\n\n"
                + String.join("\n\n--- NEUTRAL ---\n\n", neutralResponses);

        return judgeRisk(ticker, allResponses, actionContext);
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
                        "past_memory_str", TraderAgent.NO_PAST_MEMORIES_FOUND
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
}
