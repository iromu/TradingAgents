package com.embabel.gekko.agent.risk;

import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.common.ActionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.embabel.common.ai.model.ModelProvider.BEST_ROLE;

/**
 * Aggressive Risk Analyst — champions high-reward, high-risk opportunities.
 * Called directly by RiskDebateAgent via createObject(), not as a sub-process agent.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AggressiveDebator {

    @Action(description = "Produce aggressive risk argument")
    public String argue(
            String traderDecision,
            String marketResearchReport,
            String sentimentReport,
            String newsReport,
            String fundamentalsReport,
            String history,
            String currentConservativeResponse,
            String currentNeutralResponse,
            ActionContext actionContext
    ) {
        var model = Map.<String, Object>ofEntries(
                Map.entry("trader_decision", traderDecision),
                Map.entry("market_research_report", marketResearchReport),
                Map.entry("sentiment_report", sentimentReport),
                Map.entry("news_report", newsReport),
                Map.entry("fundamentals_report", fundamentalsReport),
                Map.entry("history", history),
                Map.entry("current_conservative_response", currentConservativeResponse),
                Map.entry("current_neutral_response", currentNeutralResponse)
        );

        return actionContext.ai()
                .withLlmByRole(BEST_ROLE)
                .withId("aggressiveDebator")
                .creating(String.class)
                .fromTemplate("risk/AggressiveDebator", model);
    }
}
