package com.embabel.gekko.agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.ActionContext;
import com.embabel.agent.api.common.OperationContext;
import com.embabel.agent.core.hitl.WaitFor;
import com.embabel.gekko.domain.ResearchTypes;
import com.embabel.gekko.util.FileCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.embabel.common.ai.model.ModelProvider.BEST_ROLE;

@com.embabel.agent.api.annotation.Agent(description = "Trading Orchestrator — accepts ticker input, generates research plan, runs HITL approval, delegates to DebateAgent")
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
    private final com.embabel.agent.core.Agent debateAgent;

    @Action(description = "Convert form input to Ticker object")
    public ResearchTypes.Ticker tickerFromForm(String formContent, OperationContext context) {
        String content = formContent.trim();
        if (content.isBlank()) {
            throw new IllegalArgumentException("Ticker must not be blank");
        }
        String sanitized = content.toUpperCase();
        if (!sanitized.matches("^[A-Z0-9.]+$")) {
            throw new IllegalArgumentException("Invalid ticker format: " + content);
        }
        return context.ai()
                .withLlmByRole(TICKER_MODEL)
                .withId("tickerFromForm")
                .creating(ResearchTypes.Ticker.class)
                .fromPrompt("Extract ticker from: " + sanitized);
    }

    @Action(description = "Generate a research plan for the given ticker")
    public ResearchTypes.ResearchPlan generateResearchPlan(ResearchTypes.Ticker ticker, OperationContext context) {
        String key = ticker.content() + "_research_plan";
        return cache.getOrCompute(key, ResearchTypes.ResearchPlan.class, () -> {
            String result = context.ai()
                    .withLlmByRole(BEST_ROLE)
                    .withId("generateResearchPlan")
                    .withTemplate("managers/ResearchManager")
                    .createObject(String.class, Map.of(
                            "past_memory_str", "No past memories found.",
                            "history", "",
                            "human_approved", false,
                            "user_feedback", null,
                            "ticker", ticker.content()
                    ));
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
    public ResearchTypes.InvestmentPlan executeDebate(ResearchTypes.Ticker ticker, ResearchTypes.PlanApproval approval, ActionContext context) {
        return context.asSubProcess(ResearchTypes.InvestmentPlan.class, debateAgent);
    }
}
