package com.embabel.gekko.agent.risk;

import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.common.ActionContext;
import org.springframework.stereotype.Component;

/**
 * Aggressive Risk Analyst — champions high-reward, high-risk opportunities.
 * Called directly by RiskDebateAgent via createObject(), not as a sub-process agent.
 */
@Component
public class AggressiveDebator extends BaseRiskDebator {

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
        return super.argue(traderDecision, marketResearchReport, sentimentReport, newsReport,
                fundamentalsReport, history, currentConservativeResponse, currentNeutralResponse, actionContext);
    }

    @Override
    String actionId() { return "aggressiveDebator"; }

    @Override
    String templateName() { return "risk/AggressiveDebator"; }

    @Override
    String otherKeyA() { return "current_conservative_response"; }

    @Override
    String otherKeyB() { return "current_neutral_response"; }
}
