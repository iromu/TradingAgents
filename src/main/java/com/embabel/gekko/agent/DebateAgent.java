package com.embabel.gekko.agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.ActionContext;
import com.embabel.agent.api.common.OperationContext;
import com.embabel.agent.api.common.PromptRunner;
import com.embabel.agent.core.hitl.WaitFor;
import com.embabel.gekko.domain.Analysts.FundamentalsReport;
import com.embabel.gekko.domain.Analysts.MarketReport;
import com.embabel.gekko.domain.Analysts.NewsReport;
import com.embabel.gekko.domain.Analysts.SocialMediaReport;
import com.embabel.gekko.domain.ResearchTypes;
import com.embabel.gekko.agent.managers.PortfolioManager;
import com.embabel.gekko.agent.memory.DecisionMemoryAgent;
import com.embabel.gekko.util.AgentUtils;
import com.embabel.gekko.util.LlmBudgetTracker;
import com.embabel.gekko.util.PromptSanitizer;
import com.embabel.gekko.util.ResultCache;
import com.embabel.common.textio.template.TemplateRenderer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;

import static com.embabel.common.ai.model.ModelProvider.BEST_ROLE;
import static com.embabel.common.ai.model.ModelProvider.CHEAPEST_ROLE;

/**
 * Debate Agent — orchestrates research workflow: reports, debate, risk, portfolio decision.
 * Mirrors Python TradingAgents pipeline architecture.
 */
@Agent(description = "Debate Agent — orchestrates full research workflow: reports, debate, risk, portfolio decision")
@Component
@RegisterReflectionForBinding({
        FundamentalsReport.class,
        MarketReport.class,
        NewsReport.class,
        SocialMediaReport.class,
        ResearchTypes.DebateBriefs.class,
        ResearchTypes.InvestmentDebateState.class,
        ResearchTypes.InvestmentPlan.class,
        ResearchTypes.InvestmentReviewFeedback.class
})
@RequiredArgsConstructor
@Slf4j
public class DebateAgent {

    private final ResultCache resultCache;
    private final TemplateRenderer templateRenderer;
    private final DecisionMemoryAgent memoryAgent;
    private final ObjectProvider<com.embabel.agent.core.Agent> debateLoopAgentProvider;
    private final ObjectProvider<RiskDebateAgent> riskDebateAgentProvider;
    private final ObjectProvider<Trader> traderProvider;
    private final ObjectProvider<PortfolioManager> portfolioManagerProvider;
    private final ObjectProvider<com.embabel.gekko.tools.MarketDataTools> marketDataToolsProvider;
    private final LlmBudgetTracker llmBudgetTracker;

    // Pre-compiled rating keyword patterns (find-based, case-insensitive, word-boundary)
    private static final Pattern BUY_PAT = Pattern.compile("\\bbuy\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern SELL_PAT = Pattern.compile("\\bsell\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern OVERWEIGHT_PAT = Pattern.compile("\\boverweight\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern UNDERWEIGHT_PAT = Pattern.compile("\\bunderweight\\b", Pattern.CASE_INSENSITIVE);

    private com.embabel.agent.core.Agent getDebateLoopAgent() {
        return debateLoopAgentProvider.getObject();
    }

    private RiskDebateAgent getRiskDebateAgent() {
        return riskDebateAgentProvider.getObject();
    }

    private Trader getTrader() {
        return traderProvider.getObject();
    }

    private PortfolioManager getPortfolioManager() {
        return portfolioManagerProvider.getObject();
    }

    private void trackCall(ResearchTypes.Ticker ticker) {
        if (llmBudgetTracker != null) {
            llmBudgetTracker.recordCall(ticker.content());
        }
    }

    @Action(description = "Generate fundamentals report from ticker")
    public FundamentalsReport generateFundamentalsReport(ResearchTypes.Ticker ticker, OperationContext context) {
        return generateReport(ticker, context, "fundamentals", "generateFundamentalsReport",
                "analysts/FundamentalsAnalyst", FundamentalsReport.class, FundamentalsReport::new);
    }

    @Action(description = "Generate market report from ticker")
    public MarketReport generateMarketReport(ResearchTypes.Ticker ticker, OperationContext context) {
        return generateReport(ticker, context, "market", "generateMarketReport",
                "analysts/MarketAnalyst", MarketReport.class, MarketReport::new, this::attachMarketTools);
    }

    @Action(description = "Generate news report from ticker")
    public NewsReport generateNewsReport(ResearchTypes.Ticker ticker, OperationContext context) {
        return generateReport(ticker, context, "news", "generateNewsReport",
                "analysts/NewsAnalyst", NewsReport.class, NewsReport::new);
    }

    @Action(description = "Generate social media report from ticker")
    public SocialMediaReport generateSocialMediaReport(ResearchTypes.Ticker ticker, OperationContext context) {
        return generateReport(ticker, context, "social_media", "generateSocialMediaReport",
                "analysts/SocialMediaAnalyst", SocialMediaReport.class, SocialMediaReport::new);
    }

    private <R extends ResearchTypes.Report> R generateReport(
            ResearchTypes.Ticker ticker,
            OperationContext context,
            String cacheSuffix,
            String actionId,
            String templateName,
            Class<R> reportClass,
            java.util.function.Function<String, R> reportFactory,
            java.util.function.Consumer<PromptRunner> toolAttacher
    ) {
        String key = ResultCache.canonicalKey(ResultCache.CATEGORY_LLM, ticker.content(), cacheSuffix);
        return resultCache.getOrCompute(ResultCache.CATEGORY_LLM, key, reportClass, () -> {
            trackCall(ticker);
            PromptRunner runner = context.ai()
                    .withLlmByRole(CHEAPEST_ROLE)
                    .withId(actionId);
            if (toolAttacher != null) {
                toolAttacher.accept(runner);
            }
            String result = runner.creating(String.class)
                    .fromTemplate(templateName, Map.of("ticker", ticker.content().toUpperCase()));
            return reportFactory.apply(result);
        });
    }

    private <R extends ResearchTypes.Report> R generateReport(
            ResearchTypes.Ticker ticker,
            OperationContext context,
            String cacheSuffix,
            String actionId,
            String templateName,
            Class<R> reportClass,
            java.util.function.Function<String, R> reportFactory
    ) {
        return generateReport(ticker, context, cacheSuffix, actionId, templateName, reportClass, reportFactory, null);
    }

    private void attachMarketTools(PromptRunner runner) {
        if (marketDataToolsProvider != null) {
            var marketTools = marketDataToolsProvider.getObject();
            if (marketTools != null) {
                runner.withToolObject(marketTools);
            }
        }
    }

    @Action(description = "Prepare structured debate briefs from analyst reports")
    public ResearchTypes.DebateBriefs prepareDebateBriefs(
            ResearchTypes.Ticker ticker,
            FundamentalsReport fundamentals,
            MarketReport market,
            NewsReport news,
            SocialMediaReport social,
            ActionContext actionContext
    ) {
        validateReports(ticker, fundamentals, market, news, social);

        String key = ResultCache.canonicalKey(ResultCache.CATEGORY_LLM,
                ticker.content(), "briefs");
        return resultCache.getOrCompute(ResultCache.CATEGORY_LLM, key, ResearchTypes.DebateBriefs.class, () -> {
            String fb = distill("FUNDAMENTALS", fundamentals.content(), ticker, actionContext);
            String mb = distill("MARKET", market.content(), ticker, actionContext);
            String nb = distill("NEWS", news.content(), ticker, actionContext);
            String sb = distill("SOCIAL MEDIA", social.content(), ticker, actionContext);
            if (fb.isBlank() || mb.isBlank() || nb.isBlank() || sb.isBlank()) {
                throw new IllegalStateException("One or more debate briefs are empty — distillation may have failed");
            }
            return new ResearchTypes.DebateBriefs(fb, mb, nb, sb);
        });
    }

    @Action(description = "Run iterative bull/bear debate loop via DebateLoopAgent sub-process")
    public ResearchTypes.InvestmentDebateState runDebate(ResearchTypes.Ticker ticker, ResearchTypes.DebateBriefs briefs, ActionContext actionContext) {
        return actionContext.asSubProcess(ResearchTypes.InvestmentDebateState.class, getDebateLoopAgent());
    }

    @Action(description = "Produce trader proposal from research plan")
    public String runTrader(ResearchTypes.Ticker ticker, String researchPlan, ActionContext actionContext) {
        return getTrader().traderProposal(ticker, researchPlan, actionContext);
    }

    @Action(description = "Run 3-round risk debate via RiskDebateAgent sub-process")
    public RiskAssessment runRiskDebate(
            ResearchTypes.Ticker ticker,
            ResearchTypes.DebateBriefs briefs,
            ResearchTypes.InvestmentDebateState state,
            String traderProposal,
            ActionContext actionContext
    ) {
        return getRiskDebateAgent().assessRisk(ticker, briefs, state, traderProposal, actionContext);
    }

    @Action(description = "Produce final portfolio decision from risk debate, research plan, and trader proposal")
    public String runPortfolioManager(
            ResearchTypes.Ticker ticker,
            String researchPlan,
            String traderProposal,
            ResearchTypes.InvestmentDebateState debateState,
            RiskAssessment riskAssessment,
            ActionContext actionContext
    ) {
        return getPortfolioManager().portfolioDecision(
                ticker, debateState, researchPlan, traderProposal, riskAssessment, actionContext
        );
    }

    @Action(description = "Wait for user review after debate completes")
    @AchievesGoal(description = "Wait for user review of investment debate")
    public ResearchTypes.InvestmentReviewFeedback waitForReview(ResearchTypes.InvestmentDebateState state, ResearchTypes.Ticker ticker) {
        return WaitFor.formSubmission(
                "Review the investment debate below and provide feedback, or approve to proceed with the final plan.",
                ResearchTypes.InvestmentReviewFeedback.class
        );
    }

    @Action(description = "Generate final investment plan from debate state, risk assessment, and user feedback")
    @AchievesGoal(description = "Produce final investment plan after debate, risk assessment, and user review")
    public ResearchTypes.InvestmentPlan researchManager(
            ResearchTypes.Ticker ticker,
            ResearchTypes.InvestmentDebateState state,
            RiskAssessment riskAssessment,
            ResearchTypes.InvestmentReviewFeedback feedback,
            String portfolioDecision,
            com.embabel.gekko.agent.identity.InstrumentContext instrumentContext,
            OperationContext context
    ) {
        String key = ResultCache.canonicalKey(ResultCache.CATEGORY_LLM,
                ticker.content(), "research_manager",
                riskAssessment != null ? riskAssessment.level().name() : "",
                feedback != null && feedback.approved() ? "approved" : "pending",
                feedback != null && feedback.feedback() != null ? feedback.feedback().hashCode() + "" : "");
        ResearchTypes.InvestmentPlan plan = resultCache.getOrCompute(ResultCache.CATEGORY_LLM, key, ResearchTypes.InvestmentPlan.class, () -> {
            trackCall(ticker);
            var model = buildResearchManagerModel(ticker, state, riskAssessment, feedback, portfolioDecision, instrumentContext);

            String result = context.ai()
                    .withLlmByRole(BEST_ROLE)
                    .withId("researchManager")
                    .creating(String.class)
                    .fromTemplate("managers/ResearchManager", model);
            return new ResearchTypes.InvestmentPlan(result, state);
        });

        // Store decision to memory outside cache supplier so it runs on every call, not just cache misses
        try {
            storeFinalDecision(ticker, plan, LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        } catch (Exception e) {
            log.warn("Failed to store decision to memory: {}", e.getMessage());
        }

        return plan;
    }

    private Map<String, Object> buildResearchManagerModel(
            ResearchTypes.Ticker ticker,
            ResearchTypes.InvestmentDebateState state,
            RiskAssessment riskAssessment,
            ResearchTypes.InvestmentReviewFeedback feedback,
            String portfolioDecision,
            com.embabel.gekko.agent.identity.InstrumentContext instrumentContext
    ) {
        var model = new LinkedHashMap<String, Object>();
        String pastContext;
        try {
            pastContext = memoryAgent.generatePastContext(ticker.content());
        } catch (Exception e) {
            log.warn("Failed to generate past context for {}: {}", ticker.content(), e.getMessage());
            pastContext = null;
        }
        model.put("past_memory_str", PromptSanitizer.sanitizeForPrompt(
                (pastContext != null && !pastContext.isBlank()) ? pastContext : AgentUtils.NO_PAST_MEMORY));
        model.put("history", PromptSanitizer.sanitizeForPrompt(
                state.history() != null ? String.join("\n", state.history()) : ""));
        model.put("risk_level", riskAssessment != null ? riskAssessment.level().name() : null);
        model.put("risk_reasoning", riskAssessment != null ? PromptSanitizer.sanitizeForPrompt(riskAssessment.reasoning()) : null);
        model.put("human_approved", feedback != null && feedback.approved());
        model.put("user_feedback", feedback != null && feedback.approved() && feedback.feedback() != null && !feedback.feedback().isBlank()
                ? PromptSanitizer.wrapUserFeedback(feedback.feedback())
                : null);
        model.put("ticker", PromptSanitizer.sanitizeForPrompt(ticker.content()));
        model.put("portfolio_decision", PromptSanitizer.sanitizeForPrompt(portfolioDecision));
        if (instrumentContext != null) {
            model.put("companyName", PromptSanitizer.sanitizeForPrompt(instrumentContext.companyName()));
            model.put("sector", PromptSanitizer.sanitizeForPrompt(instrumentContext.sector()));
            model.put("industry", PromptSanitizer.sanitizeForPrompt(instrumentContext.industry()));
            model.put("exchange", PromptSanitizer.sanitizeForPrompt(instrumentContext.exchange()));
        } else {
            model.put("companyName", "Unknown");
            model.put("sector", "Unknown");
            model.put("industry", "Unknown");
            model.put("exchange", "Unknown");
        }
        return model;
    }

    @Action(description = "Store the final decision to memory for future learning")
    public void storeFinalDecision(
            ResearchTypes.Ticker ticker,
            ResearchTypes.InvestmentPlan plan,
            String tradeDate
    ) {
        if (plan == null || plan.judgeDecision().isBlank()) {
            log.warn("No investment plan to store for {}", ticker.content());
            return;
        }
        try {
            String content = plan.judgeDecision();
            String rating = extractRating(content);
            String summary = extractSummary(content);
            String thesis = extractThesis(content);

            memoryAgent.storeDecision(
                    ticker.content(),
                    tradeDate,
                    rating,
                    summary,
                    thesis
            );
            log.info("Stored final decision for {} on {}", ticker.content(), tradeDate);
        } catch (Exception e) {
            log.error("Failed to store decision for {}: {}", ticker.content(), e.getMessage());
        }
    }

    private String extractRating(String content) {
        // Priority-based extraction: buy/sell > overweight/underweight > hold.
        // Uses find() with pre-compiled patterns instead of matches() scanning the whole string 4 times.
        boolean hasBuy = BUY_PAT.matcher(content).find();
        boolean hasSell = SELL_PAT.matcher(content).find();
        boolean hasOverweight = OVERWEIGHT_PAT.matcher(content).find();
        boolean hasUnderweight = UNDERWEIGHT_PAT.matcher(content).find();

        // When both buy and sell are present, check contextual cues for dominant sentiment.
        if (hasBuy && hasSell) {
            String lower = content.toLowerCase();
            // Strong buy signals that outweigh a sell mention
            if (lower.contains("strong buy") || lower.contains("buy rating")
                    || lower.contains("recommendation: buy") || lower.contains("rating: buy")
                    || lower.contains("net buy") || lower.contains("buy the dip")) {
                return "Buy";
            }
            // Strong sell signals that outweigh a buy mention
            if (lower.contains("strong sell") || lower.contains("sell rating")
                    || lower.contains("recommendation: sell") || lower.contains("rating: sell")
                    || lower.contains("net sell") || lower.contains("sell off")) {
                return "Sell";
            }
            // Default to buy when both present but no strong contextual cue
            return "Buy";
        }
        if (hasBuy) return "Buy";
        if (hasSell) return "Sell";
        if (hasOverweight) return "Overweight";
        if (hasUnderweight) return "Underweight";
        return "Hold";
    }

    private String extractSummary(String content) {
        int firstPeriod = content.indexOf(".\n");
        if (firstPeriod < 0) firstPeriod = content.indexOf(". ");
        if (firstPeriod < 0 || firstPeriod > 500) {
            return content.length() > 500 ? content.substring(0, 500) : content;
        }
        return content.substring(0, firstPeriod + 1);
    }

    private String extractThesis(String content) {
        int thesisIdx = content.toLowerCase().indexOf("thesis");
        if (thesisIdx < 0) thesisIdx = content.toLowerCase().indexOf("rationale");
        if (thesisIdx >= 0 && thesisIdx <= content.length() / 2) {
            int end = content.indexOf("\n\n", thesisIdx);
            if (end > 0 && end - thesisIdx <= 500) {
                return content.substring(thesisIdx, end);
            }
            return content.substring(thesisIdx, Math.min(thesisIdx + 500, content.length()));
        }
        // Fallback: use first paragraph (text before first double newline).
        int paraEnd = content.indexOf("\n\n");
        if (paraEnd > 0 && paraEnd <= 500) {
            return content.substring(0, paraEnd);
        }
        // Last resort: first 300 chars.
        return content.length() > 300 ? content.substring(0, 300) : content;
    }

    private void validateReports(
            ResearchTypes.Ticker ticker,
            FundamentalsReport fundamentals,
            MarketReport market,
            NewsReport news,
            SocialMediaReport social
    ) {
        requireNotBlank(ticker.content(), "Ticker");
        for (ResearchTypes.Report report : List.of(fundamentals, market, news, social)) {
            requireNotBlank(report.content(), "Report");
        }
    }

    private void requireNotBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be null or blank");
        }
    }

    private String distill(String reportType, String content, ResearchTypes.Ticker ticker, ActionContext ctx) {
        trackCall(ticker);
        return ctx.ai()
                .withLlmByRole(CHEAPEST_ROLE)
                .withId("distillBrief_" + reportType.toLowerCase().replace(" ", "_"))
                .creating(String.class)
                .fromTemplate("debate/Distiller", Map.of(
                        "reportType", reportType,
                        "ticker", ticker.content().toUpperCase(),
                        "reportContent", content
                ));
    }

}
