package com.embabel.gekko.agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.ActionContext;
import com.embabel.gekko.domain.PortfolioDecisionOutput;
import com.embabel.gekko.domain.ResearchTypes;
import com.embabel.gekko.util.AgentUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.embabel.common.ai.model.ModelProvider.BEST_ROLE;

/**
 * Portfolio Manager agent — judges the risk debate, reads the Research Manager's plan and
 * Trader's proposal, then produces the final structured PortfolioDecision.
 * Mirrors Python's create_portfolio_manager.
 */
@Agent(description = "Portfolio Manager — judges risk debate, produces final PortfolioDecision")
@Component
@RegisterReflectionForBinding(PortfolioDecisionOutput.class)
@RequiredArgsConstructor
@Slf4j
public class PortfolioManager {

    @Action(description = "Produce final portfolio decision from risk debate, research plan, and trader proposal")
    @AchievesGoal(description = "Produce final portfolio decision")
    public String portfolioDecision(
            ResearchTypes.Ticker ticker,
            String researchPlan,
            String traderProposal,
            ResearchTypes.InvestmentDebateState debateState,
            RiskAssessment riskAssessment,
            ActionContext actionContext
    ) {
        var model = Map.<String, Object>ofEntries(
                Map.entry("ticker", ticker.content().toUpperCase()),
                Map.entry("research_plan", researchPlan),
                Map.entry("trader_plan", traderProposal),
                Map.entry("risk_debate_history", debateState.history() != null ? String.join("\n", debateState.history()) : ""),
                Map.entry("risk_level", riskAssessment != null ? riskAssessment.level().name() : null),
                Map.entry("past_memory_str", AgentUtils.NO_PAST_MEMORY)
        );

        try {
            var output = actionContext.ai()
                    .withLlmByRole(BEST_ROLE)
                    .withId("portfolioManager")
                    .withTemplate("managers/PortfolioManager")
                    .createObject(PortfolioDecisionOutput.class, model);
            return output.render();
        } catch (Exception e) {
            log.warn("Structured portfolio decision failed, falling back to free-text: {}", e.getMessage());
            return actionContext.ai()
                    .withLlmByRole(BEST_ROLE)
                    .withId("portfolioManager")
                    .withTemplate("managers/PortfolioManager")
                    .createObject(String.class, model);
        }
    }
}
