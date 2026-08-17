package com.embabel.gekko.agent.risk;

import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.common.ActionContext;
import org.springframework.stereotype.Component;

/**
 * Neutral Risk Analyst — provides balanced perspective, weighs both sides.
 * Called directly by RiskDebateAgent via createObject(), not as a sub-process agent.
 */
@Component
public class NeutralDebator extends BaseRiskDebator {

    @Action(description = "Produce neutral risk argument")
    public String argue(
            String traderDecision,
            String marketResearchReport,
            String sentimentReport,
            String newsReport,
            String fundamentalsReport,
            String history,
            String currentAggressiveResponse,
            String currentConservativeResponse,
            ActionContext actionContext
    ) {
        return super.argue(traderDecision, marketResearchReport, sentimentReport, newsReport,
                fundamentalsReport, history, currentAggressiveResponse, currentConservativeResponse, actionContext);
    }

    @Override
    String actionId() { return "neutralDebator"; }

    @Override
    String templateName() { return "risk/NeutralDebator"; }

    @Override
    String otherKeyA() { return "current_aggressive_response"; }

    @Override
    String otherKeyB() { return "current_conservative_response"; }
}
