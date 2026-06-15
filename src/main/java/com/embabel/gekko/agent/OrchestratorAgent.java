package com.embabel.gekko.agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.ActionContext;
import com.embabel.agent.api.common.OperationContext;
import com.embabel.agent.core.hitl.WaitFor;
import com.embabel.gekko.domain.Analysts.FundamentalsReport;
import com.embabel.gekko.domain.Analysts.MarketReport;
import com.embabel.gekko.domain.Analysts.NewsReport;
import com.embabel.gekko.domain.Analysts.SocialMediaReport;
import com.embabel.gekko.domain.ResearchTypes;
import com.embabel.gekko.util.AgentUtils;
import com.embabel.gekko.util.FileCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.embabel.common.ai.model.ModelProvider.BEST_ROLE;
import static com.embabel.common.ai.model.ModelProvider.CHEAPEST_ROLE;

/**
 * Trading Orchestrator — accepts ticker input, generates research plan,
 * runs HITL approval, then delegates to DebateAgent for the full research workflow.
 *
 * Mirrors the Python TradingAgents entry point that kicks off the full agent pipeline.
 */
@Agent(description = "Trading Orchestrator — accepts ticker input, generates research plan, runs HITL approval, delegates to DebateAgent")
@Component
@RegisterReflectionForBinding({
        ResearchTypes.Ticker.class,
        ResearchTypes.PlanApproval.class,
        ResearchTypes.ResearchPlan.class,
        ResearchTypes.InvestmentPlan.class
})
@RequiredArgsConstructor
@Slf4j
public class OrchestratorAgent {

    public static final String TICKER_MODEL = BEST_ROLE;

    private final FileCache cache;
    private final ObjectProvider<com.embabel.agent.core.Agent> debateAgentProvider;

    private com.embabel.agent.core.Agent getDebateAgent() {
        var agent = debateAgentProvider.getObject();
        if (agent == null) {
            throw new IllegalStateException("No DebateAgent registered — check that DebateAgent has a @Component annotation");
        }
        return agent;
    }

    @Action(description = "Convert form input to Ticker object")
    public ResearchTypes.Ticker tickerFromForm(com.embabel.gekko.web.TradingHtmxController.TickerForm form, OperationContext context) {
        String content = form.getContent().trim();
        if (content.isBlank()) {
            throw new IllegalArgumentException("Ticker must not be blank");
        }
        String sanitized = content.toUpperCase();
        if (!sanitized.matches("^[A-Z0-9.]+$")) {
            throw new IllegalArgumentException("Invalid ticker format: " + content);
        }
        return new ResearchTypes.Ticker(sanitized, "");
    }

    @Action(description = "Generate a research plan for the given ticker")
    public ResearchTypes.ResearchPlan generateResearchPlan(ResearchTypes.Ticker ticker, OperationContext context) {
        String key = ticker.content() + "_research_plan";
        return cache.getOrCompute(key, ResearchTypes.ResearchPlan.class, () -> {
            Map<String, Object> model = new java.util.HashMap<>();
            model.put("past_memory_str", AgentUtils.NO_PAST_MEMORY);
            model.put("history", "");
            model.put("human_approved", false);
            model.put("user_feedback", "");
            model.put("ticker", ticker.content());
            String result = context.ai()
                    .withLlmByRole(BEST_ROLE)
                    .withId("generateResearchPlan")
                    .withTemplate("managers/ResearchManager")
                    .createObject(String.class, model);
            return new ResearchTypes.ResearchPlan(result);
        });
    }

    @Action(description = "Wait for user to review and approve the research plan")
    public ResearchTypes.PlanApproval waitForPlanApproval(ResearchTypes.ResearchPlan researchPlan, ResearchTypes.Ticker ticker) {
        return WaitFor.formSubmission(
                "Review the research plan below and provide feedback, or approve to execute the full research workflow.",
                ResearchTypes.PlanApproval.class
        );
    }

    @Action(description = "Delegate to DebateAgent for full research workflow")
    @AchievesGoal(description = "Execute full research workflow with debate and risk assessment")
    public ResearchTypes.InvestmentPlan executeDebate(ResearchTypes.Ticker ticker, ResearchTypes.PlanApproval approval, ActionContext context) {
        return context.asSubProcess(ResearchTypes.InvestmentPlan.class, getDebateAgent());
    }
}
