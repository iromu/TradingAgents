package com.embabel.gekko.agent.risk;

import com.embabel.agent.api.common.ActionContext;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.embabel.common.ai.model.ModelProvider.BEST_ROLE;

abstract class BaseRiskDebator {

    protected String argue(
            String traderDecision,
            String marketResearchReport,
            String sentimentReport,
            String newsReport,
            String fundamentalsReport,
            String history,
            String currentResponseA,
            String currentResponseB,
            ActionContext actionContext
    ) {
        var model = new LinkedHashMap<String, Object>();
        model.put("trader_decision", traderDecision);
        model.put("market_research_report", marketResearchReport);
        model.put("sentiment_report", sentimentReport);
        model.put("news_report", newsReport);
        model.put("fundamentals_report", fundamentalsReport);
        model.put("history", history);
        model.put(otherKeyA(), currentResponseA);
        model.put(otherKeyB(), currentResponseB);

        return actionContext.ai()
                .withLlmByRole(BEST_ROLE)
                .withId(actionId())
                .creating(String.class)
                .fromTemplate(templateName(), model);
    }

    abstract String actionId();

    abstract String templateName();

    abstract String otherKeyA();

    abstract String otherKeyB();
}
