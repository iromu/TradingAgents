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
import com.embabel.gekko.util.FileCache;
import com.embabel.gekko.util.AgentUtils;
import com.embabel.common.textio.template.TemplateRenderer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;

import static com.embabel.common.ai.model.ModelProvider.BEST_ROLE;
import static com.embabel.common.ai.model.ModelProvider.CHEAPEST_ROLE;

@Agent(description = "Debate Agent — generates analyst reports, runs debate loop, risk debate, HITL review, and final plan")
@Component
@RegisterReflectionForBinding({
        FundamentalsReport.class,
        MarketReport.class,
        NewsReport.class,
        SocialMediaReport.class,
        ResearchTypes.DebateBriefs.class,
        ResearchTypes.InvestmentDebateState.class,
        RiskAssessment.class,
        ResearchTypes.InvestmentReviewFeedback.class,
        ResearchTypes.InvestmentPlan.class
})
@RequiredArgsConstructor
@Slf4j
public class DebateAgent {

    private final FileCache cache;
    private final TemplateRenderer templateRenderer;
    private final ObjectProvider<com.embabel.agent.core.Agent> debateLoopAgentProvider;
    private final ObjectProvider<com.embabel.agent.core.Agent> riskDebateAgentProvider;

    private com.embabel.agent.core.Agent getDebateLoopAgent() {
        return debateLoopAgentProvider.getObject();
    }

    private com.embabel.agent.core.Agent getRiskDebateAgent() {
        return riskDebateAgentProvider.getObject();
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
        if (ticker.content() == null || ticker.content().isBlank()) {
            throw new IllegalArgumentException("Ticker must not be blank");
        }
        if (fundamentals.content() == null || fundamentals.content().isBlank()) {
            throw new IllegalArgumentException("Fundamentals report must not be null or blank");
        }
        if (market.content() == null || market.content().isBlank()) {
            throw new IllegalArgumentException("Market report must not be null or blank");
        }
        if (news.content() == null || news.content().isBlank()) {
            throw new IllegalArgumentException("News report must not be null or blank");
        }
        if (social.content() == null || social.content().isBlank()) {
            throw new IllegalArgumentException("Social media report must not be null or blank");
        }
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

    @Action(description = "Run iterative bull/bear debate loop via DebateLoopAgent")
    public ResearchTypes.InvestmentDebateState runDebate(ResearchTypes.Ticker ticker, ResearchTypes.DebateBriefs briefs, ActionContext actionContext) {
        return actionContext.asSubProcess(ResearchTypes.InvestmentDebateState.class, getDebateLoopAgent());
    }

    @Action(description = "Run 3-round risk debate via RiskDebateAgent")
    public RiskAssessment runRiskDebate(ResearchTypes.Ticker ticker, ResearchTypes.DebateBriefs briefs, ResearchTypes.InvestmentDebateState state, ActionContext actionContext) {
        return actionContext.asSubProcess(RiskAssessment.class, getRiskDebateAgent());
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
            OperationContext context
    ) {
        String key = ticker.content() + "_research_manager";
        return cache.getOrCompute(key, ResearchTypes.InvestmentPlan.class, () -> {
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

            String result = context.ai()
                    .withLlmByRole(BEST_ROLE)
                    .withId("researchManager")
                    .withTemplate("managers/ResearchManager")
                    .createObject(String.class, model);
            return new ResearchTypes.InvestmentPlan(result, state);
        });
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
        String sanitized = input
                // Block complete Jinja variable expressions: {{ ... }}
                .replaceAll("(?s)\\{\\{.*?\\}\\}", "[BLOCKED_TEMPLATE]")
                // Block complete Jinja statement expressions: {% ... %}
                .replaceAll("(?s)\\{%.*?%\\}", "[BLOCKED_TEMPLATE]")
                // Block unclosed Jinja variable: {{ without matching }}
                .replaceAll("(?s)\\{\\{[^}]*$", "[BLOCKED_TEMPLATE]")
                // Block unclosed Jinja statement: {% without matching %}
                .replaceAll("(?s)\\{%[^%]*$", "[BLOCKED_TEMPLATE]")
                // Block markdown code fences (triple backtick blocks)
                .replaceAll("(?s)```[\\s\\S]*?```", "[BLOCKED_CODE]")
                // Block unclosed code fence
                .replaceAll("(?s)```.*$", "[BLOCKED_CODE]");

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

        if (sanitized.length() > 1000) {
            sanitized = sanitized.substring(0, 1000) + "...[truncated]";
        }
        return "<user_feedback>\n" + sanitized + "\n</user_feedback>";
    }
}
