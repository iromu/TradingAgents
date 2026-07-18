package com.embabel.gekko.agent;

import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.ActionContext;
import com.embabel.agent.api.common.OperationContext;
import com.embabel.agent.core.hitl.WaitFor;
import com.embabel.gekko.agent.identity.InstrumentContext;
import com.embabel.gekko.agent.identity.InstrumentContextPromptContributor;
import com.embabel.gekko.agent.identity.InstrumentIdentityAgent;
import com.embabel.gekko.agent.checkpoint.CheckpointAgent;
import com.embabel.gekko.agent.memory.DecisionMemoryAgent;
import com.embabel.gekko.domain.Analysts.FundamentalsReport;
import com.embabel.gekko.domain.Analysts.MarketReport;
import com.embabel.gekko.domain.Analysts.NewsReport;
import com.embabel.gekko.domain.Analysts.SocialMediaReport;
import com.embabel.gekko.domain.ResearchTypes;
import com.embabel.gekko.util.AgentUtils;
import com.embabel.gekko.util.FileCache;
import com.embabel.gekko.util.LlmBudgetTracker;
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
        ResearchTypes.InvestmentPlan.class,
        InstrumentContext.class
})
@RequiredArgsConstructor
@Slf4j
public class OrchestratorAgent {

    public static final String TICKER_MODEL = BEST_ROLE;

    private final FileCache cache;
    private final InstrumentIdentityAgent identityAgent;
    private final DecisionMemoryAgent memoryAgent;
    private final CheckpointAgent checkpointAgent;
    private final InstrumentContextPromptContributor instrumentContextContributor;
    private final ObjectProvider<com.embabel.agent.core.Agent> debateAgentProvider;
    private final LlmBudgetTracker llmBudgetTracker;

    private com.embabel.agent.core.Agent getDebateAgent() {
        var agent = debateAgentProvider.getObject();
        if (agent == null) {
            throw new IllegalStateException("No DebateAgent registered — check that DebateAgent has a @Component annotation");
        }
        return agent;
    }

    private void trackCall(String ticker) {
        if (llmBudgetTracker != null) {
            llmBudgetTracker.recordCall(ticker);
        }
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

    @Action(description = "Resolve ticker to real company identity (name, sector, industry, exchange)")
    public InstrumentContext resolveIdentity(ResearchTypes.Ticker ticker, OperationContext context) {
        InstrumentContext contextResult = identityAgent.resolveIdentity(ticker);
        if (contextResult != null) {
            log.info("Instrument identity resolved: {} → {}", ticker.content(), contextResult.companyName());
            instrumentContextContributor.setContext(contextResult);
        } else {
            log.warn("Instrument identity resolution failed for {}, continuing without context", ticker.content());
            instrumentContextContributor.setContext(null);
        }
        return contextResult;
    }

    @Action(description = "Generate a research plan for the given ticker")
    public ResearchTypes.ResearchPlan generateResearchPlan(
            ResearchTypes.Ticker ticker,
            InstrumentContext instrumentContext,
            OperationContext context) {
        String key = ticker.content() + "_research_plan";
        return cache.getOrCompute(key, ResearchTypes.ResearchPlan.class, () -> {
            trackCall(ticker.content());
            var model = buildResearchPlanModel(ticker, instrumentContext);

            String result = context.ai()
                    .withLlmByRole(BEST_ROLE)
                    .withId("generateResearchPlan")
                    .withTemplate("managers/ResearchManager")
                    .createObject(String.class, model);
            return new ResearchTypes.ResearchPlan(result);
        });
    }

    private Map<String, Object> buildResearchPlanModel(
            ResearchTypes.Ticker ticker,
            InstrumentContext instrumentContext
    ) {
        var model = new java.util.HashMap<String, Object>();
        model.put("past_memory_str", generatePastContext(ticker));
        model.put("history", "");
        model.put("human_approved", false);
        model.put("user_feedback", "");
        model.put("ticker", ticker.content());
        if (instrumentContext != null) {
            model.put("companyName", instrumentContext.companyName());
            model.put("sector", instrumentContext.sector());
            model.put("industry", instrumentContext.industry());
            model.put("exchange", instrumentContext.exchange());
        }
        return model;
    }

    @Action(description = "Wait for user to review and approve the research plan")
    public ResearchTypes.PlanApproval waitForPlanApproval(ResearchTypes.ResearchPlan researchPlan, ResearchTypes.Ticker ticker) {
        return WaitFor.formSubmission(
                "Review the research plan below and provide feedback, or approve to execute the full research workflow.",
                ResearchTypes.PlanApproval.class
        );
    }

    @Action(description = "Resolve any pending decisions for this ticker from previous runs")
    public void resolvePendingDecisions(ResearchTypes.Ticker ticker, String tradeDate) {
        try {
            memoryAgent.resolvePending(ticker.content(), tradeDate, null);
            log.info("Resolved pending decisions for {}", ticker.content());
        } catch (Exception e) {
            log.error("Failed to resolve pending decisions for {}: {}", ticker.content(), e.getMessage());
        }
    }

    @Action(description = "Generate past context from decision memory for injection into PM prompt")
    public String generatePastContext(ResearchTypes.Ticker ticker) {
        try {
            String context = memoryAgent.generatePastContext(ticker.content());
            if (context != null && !context.isBlank()) {
                log.info("Generated past context for {} ({} chars)", ticker.content(), context.length());
                return context;
            }
            return AgentUtils.NO_PAST_MEMORY;
        } catch (Exception e) {
            log.error("Failed to generate past context for {}: {}", ticker.content(), e.getMessage(), e);
            return AgentUtils.NO_PAST_MEMORY;
        }
    }

    @Action(description = "Delegate to DebateAgent for full research workflow")
    public ResearchTypes.InvestmentPlan executeDebate(ResearchTypes.Ticker ticker, ResearchTypes.PlanApproval approval, ActionContext context) {
        return context.asSubProcess(ResearchTypes.InvestmentPlan.class, getDebateAgent());
    }
}
