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
import com.embabel.gekko.agent.managers.PortfolioManager;
import com.embabel.gekko.agent.memory.DecisionMemoryAgent;
import com.embabel.gekko.util.AgentUtils;
import com.embabel.gekko.util.FileCache;
import com.embabel.common.textio.template.TemplateRenderer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;

import static com.embabel.common.ai.model.ModelProvider.BEST_ROLE;
import static com.embabel.common.ai.model.ModelProvider.CHEAPEST_ROLE;

/**
 * Debate Agent — orchestrates the full research workflow:
 * 1. Generate 4 analyst reports (fundamentals, market, news, social media)
 * 2. Distill reports into debate briefs
 * 3. Run bull/bear debate loop (DebateLoopAgent sub-process)
 * 4. Research Manager produces investment plan
 * 5. Trader produces transaction proposal
 * 6. Risk Debate Agent assesses risk (sub-process)
 * 7. Portfolio Manager produces final decision
 * 8. HITL review
 * 9. Final investment plan
 *
 * Mirrors the Python TradingAgents pipeline architecture.
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

    private final FileCache cache;
    private final TemplateRenderer templateRenderer;
    private final DecisionMemoryAgent memoryAgent;
    private final ObjectProvider<com.embabel.agent.core.Agent> debateLoopAgentProvider;
    private final ObjectProvider<RiskDebateAgent> riskDebateAgentProvider;
    private final ObjectProvider<Trader> traderProvider;
    private final ObjectProvider<PortfolioManager> portfolioManagerProvider;

    // Pre-compiled regex patterns for input sanitization (ReDoS mitigation)
    private static final Pattern JINJA_VAR = Pattern.compile("(?s)\\{\\{.*?\\}\\}");
    private static final Pattern JINJA_STMT = Pattern.compile("(?s)\\{%.*?%\\}");
    private static final Pattern JINJA_VAR_UNCLOSED = Pattern.compile("(?s)\\{\\{[^}]*$");
    private static final Pattern JINJA_STMT_UNCLOSED = Pattern.compile("(?s)\\{%[^%]*$");
    private static final Pattern CODE_FENCE = Pattern.compile("(?s)```[\\s\\S]*?```");
    private static final Pattern CODE_FENCE_UNCLOSED = Pattern.compile("(?s)```.*$");

    private static final int MAX_INPUT_LENGTH = 10000;
    private static final int MAX_OUTPUT_LENGTH = 1000;

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

    @Action(description = "Generate fundamentals report from ticker")
    public FundamentalsReport generateFundamentalsReport(ResearchTypes.Ticker ticker, OperationContext context) {
        String key = ticker.content() + "_fundamentals";
        return cache.getOrCompute(key, FundamentalsReport.class, () -> {
            String result = context.ai()
                    .withLlmByRole(CHEAPEST_ROLE)
                    .withId("generateFundamentalsReport")
                    .withTemplate("analysts/FundamentalsAnalyst")
                    .createObject(String.class, Map.of(
                            "ticker", ticker.content().toUpperCase()
                    ));
            return new FundamentalsReport(result);
        });
    }

    @Action(description = "Generate market report from ticker")
    public MarketReport generateMarketReport(ResearchTypes.Ticker ticker, OperationContext context) {
        String key = ticker.content() + "_market";
        return cache.getOrCompute(key, MarketReport.class, () -> {
            String result = context.ai()
                    .withLlmByRole(CHEAPEST_ROLE)
                    .withId("generateMarketReport")
                    .withTemplate("analysts/MarketAnalyst")
                    .createObject(String.class, Map.of(
                            "ticker", ticker.content().toUpperCase()
                    ));
            return new MarketReport(result);
        });
    }

    @Action(description = "Generate news report from ticker")
    public NewsReport generateNewsReport(ResearchTypes.Ticker ticker, OperationContext context) {
        String key = ticker.content() + "_news";
        return cache.getOrCompute(key, NewsReport.class, () -> {
            String result = context.ai()
                    .withLlmByRole(CHEAPEST_ROLE)
                    .withId("generateNewsReport")
                    .withTemplate("analysts/NewsAnalyst")
                    .createObject(String.class, Map.of(
                            "ticker", ticker.content().toUpperCase()
                    ));
            return new NewsReport(result);
        });
    }

    @Action(description = "Generate social media report from ticker")
    public SocialMediaReport generateSocialMediaReport(ResearchTypes.Ticker ticker, OperationContext context) {
        String key = ticker.content() + "_social_media";
        return cache.getOrCompute(key, SocialMediaReport.class, () -> {
            String result = context.ai()
                    .withLlmByRole(CHEAPEST_ROLE)
                    .withId("generateSocialMediaReport")
                    .withTemplate("analysts/SocialMediaAnalyst")
                    .createObject(String.class, Map.of(
                            "ticker", ticker.content().toUpperCase()
                    ));
            return new SocialMediaReport(result);
        });
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

        String key = ticker.content() + "_briefs";
        return cache.getOrCompute(key, ResearchTypes.DebateBriefs.class, () -> {
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
    public ResearchTypes.InvestmentReviewFeedback waitForReview(ResearchTypes.InvestmentDebateState state, ResearchTypes.Ticker ticker) {
        return WaitFor.formSubmission(
                "Review the investment debate below and provide feedback, or approve to proceed with the final plan.",
                ResearchTypes.InvestmentReviewFeedback.class
        );
    }

    @Action(description = "Generate final investment plan from debate state, risk assessment, and user feedback")
    @AchievesGoal(description = "Generate final investment plan")
    public ResearchTypes.InvestmentPlan researchManager(
            ResearchTypes.Ticker ticker,
            ResearchTypes.InvestmentDebateState state,
            RiskAssessment riskAssessment,
            ResearchTypes.InvestmentReviewFeedback feedback,
            String portfolioDecision,
            OperationContext context
    ) {
        String key = ticker.content() + "_research_manager";
        return cache.getOrCompute(key, ResearchTypes.InvestmentPlan.class, () -> {
            var model = buildResearchManagerModel(ticker, state, riskAssessment, feedback, portfolioDecision);

            String result = context.ai()
                    .withLlmByRole(BEST_ROLE)
                    .withId("researchManager")
                    .withTemplate("managers/ResearchManager")
                    .createObject(String.class, model);
            var plan = new ResearchTypes.InvestmentPlan(result, state);

            // Store decision to memory after plan is generated
            try {
                storeFinalDecision(ticker, plan, "auto");
            } catch (Exception e) {
                log.warn("Failed to store decision to memory: {}", e.getMessage());
            }

            return plan;
        });
    }

    private Map<String, Object> buildResearchManagerModel(
            ResearchTypes.Ticker ticker,
            ResearchTypes.InvestmentDebateState state,
            RiskAssessment riskAssessment,
            ResearchTypes.InvestmentReviewFeedback feedback,
            String portfolioDecision
    ) {
        var model = new LinkedHashMap<String, Object>();
        model.put("past_memory_str", AgentUtils.NO_PAST_MEMORY);
        model.put("history", String.join("\n", state.history()));
        model.put("risk_level", riskAssessment != null ? riskAssessment.level().name() : null);
        model.put("risk_reasoning", riskAssessment != null ? riskAssessment.reasoning() : null);
        model.put("human_approved", feedback != null && feedback.approved());
        model.put("user_feedback", feedback != null && feedback.approved() && feedback.feedback() != null && !feedback.feedback().isBlank()
                ? sanitizeForPrompt(feedback.feedback())
                : null);
        model.put("ticker", ticker.content());
        model.put("portfolio_decision", portfolioDecision);
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
        // Use word-boundary regex to avoid false positives (e.g., "not a buy" should not match)
        if (content.matches("(?i).*\\bbuy\\b.*")) return "Buy";
        if (content.matches("(?i).*\\bsell\\b.*")) return "Sell";
        if (content.matches("(?i).*\\boverweight\\b.*")) return "Overweight";
        if (content.matches("(?i).*\\bunderweight\\b.*")) return "Underweight";
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
        if (thesisIdx < 0 || thesisIdx > content.length() / 2) {
            return content.length() > 300 ? content.substring(0, 300) : content;
        }
        int end = content.indexOf("\n\n", thesisIdx);
        if (end < 0 || end - thesisIdx > 500) {
            return content.substring(thesisIdx, Math.min(thesisIdx + 500, content.length()));
        }
        return content.substring(thesisIdx, end);
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
        return ctx.ai()
                .withLlmByRole(CHEAPEST_ROLE)
                .withId("distillBrief_" + reportType.toLowerCase().replace(" ", "_"))
                .withTemplate("debate/Distiller")
                .createObject(String.class, Map.of(
                        "reportType", reportType,
                        "ticker", ticker.content().toUpperCase(),
                        "reportContent", content
                ));
    }

    private String sanitizeForPrompt(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        // Reject oversized input before regex processing (ReDoS mitigation)
        if (input.length() > MAX_INPUT_LENGTH) {
            input = input.substring(0, MAX_INPUT_LENGTH);
        }
        String sanitized = JINJA_VAR.matcher(input).replaceAll("[BLOCKED_TEMPLATE]")
                .replaceAll(JINJA_STMT.pattern(), "[BLOCKED_TEMPLATE]")
                .replaceAll(JINJA_VAR_UNCLOSED.pattern(), "[BLOCKED_TEMPLATE]")
                .replaceAll(JINJA_STMT_UNCLOSED.pattern(), "[BLOCKED_TEMPLATE]")
                .replaceAll(CODE_FENCE.pattern(), "[BLOCKED_CODE]")
                .replaceAll(CODE_FENCE_UNCLOSED.pattern(), "[BLOCKED_CODE]");

        StringBuilder sb = new StringBuilder(sanitized.length());
        for (int i = 0; i < sanitized.length(); i++) {
            char c = sanitized.charAt(i);
            if (c == '\t' || c == '\n' || c == '\r') {
                sb.append(c);
            } else if (c >= 0x20 && Character.isISOControl(c) == false) {
                sb.append(c);
            }
        }
        sanitized = sb.toString();

        if (sanitized.length() > MAX_OUTPUT_LENGTH) {
            sanitized = sanitized.substring(0, MAX_OUTPUT_LENGTH) + "...[truncated]";
        }
        return "<user_feedback>\n" + sanitized + "\n</user_feedback>";
    }
}
