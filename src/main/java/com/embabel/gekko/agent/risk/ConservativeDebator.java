package com.embabel.gekko.agent.risk;

import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.common.ActionContext;
import org.springframework.stereotype.Component;

/**
 * Conservative Risk Analyst — prioritizes capital preservation, minimizes risk.
 * Called directly by RiskDebateAgent via createObject(), not as a sub-process agent.
 */
@Component
public class ConservativeDebator extends BaseRiskDebator {

    @Action(description = "Produce conservative risk argument")
    public String argue(
            String traderDecision,
            String marketResearchReport,
            String sentimentReport,
            String newsReport,
            String fundamentalsReport,
            String history,
            String currentAggressiveResponse,
            String currentNeutralResponse,
            ActionContext actionContext
    ) {
        return super.argue(traderDecision, marketResearchReport, sentimentReport, newsReport,
                fundamentalsReport, history, currentAggressiveResponse, currentNeutralResponse, actionContext);
    }

    @Override
    String actionId() { return "conservativeDebator"; }

    @Override
    String templateName() { return "risk/ConservativeDebator"; }

    @Override
    String otherKeyA() { return "current_aggressive_response"; }

    @Override
    String otherKeyB() { return "current_neutral_response"; }
}
