package com.embabel.gekko.agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.ActionContext;
import com.embabel.agent.api.common.OperationContext;
import com.embabel.agent.api.common.PromptRunner;
import com.embabel.agent.api.common.workflow.loop.Feedback;
import com.embabel.agent.api.common.workflow.loop.RepeatUntilBuilder;
import com.embabel.agent.api.common.streaming.StreamingPromptRunner;
import com.embabel.agent.core.hitl.WaitFor;
import com.embabel.agent.domain.io.UserInput;
import com.embabel.gekko.agent.researchers.BearResearcher;
import com.embabel.gekko.agent.researchers.BullResearcher;
import com.embabel.common.ai.model.LlmOptions;
import com.embabel.common.textio.template.TemplateRenderer;
import com.embabel.gekko.config.TraderAgentConfig;
import com.embabel.gekko.domain.Analysts.FundamentalsReport;
import com.embabel.gekko.domain.Analysts.MarketReport;
import com.embabel.gekko.domain.Analysts.NewsReport;
import com.embabel.gekko.domain.Analysts.SocialMediaReport;
import com.embabel.gekko.tools.FundamentalDataTools;
import com.embabel.gekko.tools.MarketDataTools;
import com.embabel.gekko.tools.NewsDataTools;
import com.embabel.gekko.util.FileCache;
import com.embabel.gekko.web.TradingHtmxController;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.stream.Collectors;

import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import static com.embabel.common.ai.model.ModelProvider.BEST_ROLE;
import static com.embabel.common.ai.model.ModelProvider.CHEAPEST_ROLE;

@Agent(description = "Trading Agent")
@RegisterReflectionForBinding({
        com.embabel.gekko.domain.Analysts.FundamentalsReport.class,
        com.embabel.gekko.domain.Analysts.MarketReport.class,
        com.embabel.gekko.domain.Analysts.NewsReport.class,
        com.embabel.gekko.domain.Analysts.SocialMediaReport.class,
        com.embabel.gekko.agent.TraderAgent.DebateBriefs.class,
        com.embabel.gekko.agent.TraderAgent.InvestmentDebateState.class,
        com.embabel.gekko.agent.TraderAgent.InvestmentDebateFeedback.class,
        com.embabel.gekko.agent.TraderAgent.InvestmentReviewFeedback.class,
        com.embabel.gekko.agent.TraderAgent.Ticker.class,
        com.embabel.gekko.agent.TraderAgent.PlanApproval.class,
        com.embabel.gekko.agent.TraderAgent.ResearchPlan.class,
        com.embabel.gekko.agent.RiskAssessment.class,
        com.embabel.gekko.agent.RiskLevel.class
})
@RequiredArgsConstructor
@Slf4j
public class TraderAgent {
    public static final String NO_PAST_MEMORIES_FOUND = "No past memories found.";
    @Value("classpath:prompts/analysts/FundamentalsAnalyst.jinja")
    private Resource promptFundamentalsAnalyst;

    @Value("classpath:prompts/analysts/MarketAnalyst.jinja")
    private Resource promptMarketAnalyst;

    @Value("classpath:prompts/analysts/NewsAnalyst.jinja")
    private Resource promptNewsAnalyst;

    @Value("classpath:prompts/analysts/SocialMediaAnalyst.jinja")
    private Resource promptSocialMediaAnalyst;

    private final FileCache cache;
    private final MarketDataTools marketDataTools;
    private final FundamentalDataTools fundamentalDataTools;
    private final NewsDataTools newsDataTools;

    private final TraderAgentConfig config;
    private final BullResearcher bullAgent;
    private final BearResearcher bearAgent;
    private final RiskDebateService riskDebateService;
    private final TemplateRenderer templateRenderer;

    public interface Report {
        String content();
    }


    public record DebateBriefs(
            String fundamentalsBrief,
            String marketBrief,
            String newsBrief,
            String socialBrief
    ) implements Report {
        @Override
        public String content() {
            return "### FUNDAMENTALS BRIEF\n\n" + fundamentalsBrief
                    + "\n\n### MARKET BRIEF\n\n" + marketBrief
                    + "\n\n### NEWS BRIEF\n\n" + newsBrief
                    + "\n\n### SOCIAL BRIEF\n\n" + socialBrief;
        }
    }

    public record InvestmentDebateState(List<String> history, List<String> bullHistory, List<String> bearHistory,
                                        String currentResponse,
                                        int count,
                                        DebateBriefs briefs,
                                        RiskAssessment riskAssessment) implements Report {
        @Override
        public String content() {
            return currentResponse;
        }
    }

    public record InvestmentPlan(String judgeDecision, InvestmentDebateState investmentDebateState) implements Report {
        @Override
        public String content() {
            return judgeDecision;
        }
    }

    public record InvestmentDebateFeedback(List<String> history, List<String> bullHistory, List<String> bearHistory,
                                           String currentResponse,
                                           int count,
                                           DebateBriefs briefs,
                                           RiskAssessment riskAssessment) implements Feedback, Report {
        @Override
        public double getScore() {
            return 0;
        }

        @Override
        public String content() {
            return currentResponse;
        }
    }

    public record Ticker(String content, String feedback) {
    }

    /**
     * Human feedback captured via WaitFor (pre-execution HITL checkpoint).
     * Used between the debate and the final investment plan.
     */
    public record InvestmentReviewFeedback(
            String feedback,
            boolean approved
    ) {
    }

    /**
     * A research plan generated by the ResearchManager before the full debate workflow.
     * Presented to the user for review and approval before execution.
     */
    public record ResearchPlan(String content) implements Report {
        @Override
        public String content() {
            return content;
        }
    }

    /**
     * Human-in-the-loop approval for a generated research plan.
     * Captured via WaitFor.formSubmission() between plan generation and execution.
     */
    public record PlanApproval(
            String feedback,
            boolean approved
    ) {
    }

    @Action(description = "Convert user input to Ticker object")
    public Ticker tickerFromUserInput(UserInput userInput, OperationContext context) {
        String key = userInput.getContent() + "_ticker";
        return cache.getOrCompute(key, Ticker.class, () -> {
            String input = userInput.getContent();
            if (input == null || input.isBlank()) {
                throw new IllegalArgumentException("User input must not be blank");
            }
            String prompt = """
                    Extract ticker from this user input:
                    %s
                    """.formatted(input);
            PromptRunner runner = context.ai()
                    .withLlmByRole(CHEAPEST_ROLE)
                    .withId("extractTicker");
            if (runner.supportsStreaming()) {
                Flux<String> stream = ((StreamingPromptRunner) runner).streaming()
                        .withPrompt(prompt)
                        .generateStream()
                        .subscribeOn(Schedulers.boundedElastic());
                List<String> chunks = stream.collectList()
                        .block(java.time.Duration.ofSeconds(30));
                if (chunks == null || chunks.isEmpty()) {
                    throw new IllegalStateException("LLM stream returned no chunks for ticker extraction");
                }
                String result = chunks.stream().collect(Collectors.joining());
                Ticker ticker = parseTicker(result);
                // Attach empty feedback — user input path doesn't carry feedback
                return new Ticker(ticker.content(), "");
            } else {
                Ticker ticker = runner.creating(Ticker.class).fromPrompt(prompt);
                return new Ticker(ticker.content(), "");
            }
        });
    }

    @Action(description = "Convert form input to Ticker object")
    public Ticker tickerFromForm(TradingHtmxController.TickerForm tickerForm) {
        String content = tickerForm.getContent();
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Ticker must not be blank");
        }
        // Validate ticker format: must be alphanumeric, 1-5 characters (standard ticker format)
        String sanitized = content.trim().toUpperCase();
        if (!sanitized.matches("^[A-Z0-9.]+$")) {
            throw new IllegalArgumentException("Invalid ticker format: " + content + " — must be alphanumeric (dots allowed for ETFs like SPY.X)");
        }
        if (sanitized.length() > 10) {
            throw new IllegalArgumentException("Ticker too long: " + content + " — max 10 characters");
        }
        // Pass user feedback through to the agent's context so subsequent actions can use it
        String feedback = tickerForm.getFeedback();
        if (feedback == null || feedback.isBlank()) {
            feedback = "";
        }
        return new Ticker(sanitized, feedback);
    }

    @Action(description = "Generate Fundamentals Report")
    public FundamentalsReport generateFundamentalsReport(Ticker ticker, OperationContext context) {

        String key = ticker.content() + "_fundamentals";
        return cache.getOrCompute(key, FundamentalsReport.class, () -> {
            try {
                PromptRunner runner = context.ai()
                        .withLlmByRole(CHEAPEST_ROLE)
                        .withId("generateFundamentalsReport")
                        .withToolObject(fundamentalDataTools);
                String result = streamWithTemplate(runner, "analysts/_BaseAnalyst", Map.of(
                        "tool_names", "get_fundamentals,get_balance_sheet,get_cashflow,get_income_statement",
                        "system_message", promptFundamentalsAnalyst.getContentAsString(StandardCharsets.UTF_8),
                        "ticker", ticker.content().toUpperCase()
                ));
                return new FundamentalsReport(result);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Action(description = "Generate Market Report")
    public MarketReport generateMarketReport(Ticker ticker, OperationContext context) {
        String key = ticker.content() + "_market";
        return cache.getOrCompute(key, MarketReport.class, () -> {
            try {
                PromptRunner runner = context.ai()
                        .withLlmByRole(CHEAPEST_ROLE)
                        .withId("generateMarketReport");
                String result = streamWithTemplate(runner, "analysts/_BaseAnalyst", Map.of(
                        "tool_names", "get_stock_data,get_indicators",
                        "system_message", promptMarketAnalyst.getContentAsString(StandardCharsets.UTF_8),
                        "ticker", ticker.content().toUpperCase()
                ));
                return new MarketReport(result);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Action(description = "Generate News Report")
    public NewsReport generateNewsReport(Ticker ticker, OperationContext context) {
        String key = ticker.content() + "_news";
        return cache.getOrCompute(key, NewsReport.class, () -> {
            try {
                PromptRunner runner = context.ai()
                        .withLlmByRole(CHEAPEST_ROLE)
                        .withId("generateNewsReport")
                        .withToolObject(newsDataTools);
                String result = streamWithTemplate(runner, "analysts/_BaseAnalyst", Map.of(
                        "tool_names", "get_news,get_global_news",
                        "system_message", promptNewsAnalyst.getContentAsString(StandardCharsets.UTF_8),
                        "ticker", ticker.content().toUpperCase()
                ));
                return new NewsReport(result);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Action(description = "Generate Social Media Report")
    public SocialMediaReport generateSocialMediaReport(Ticker ticker, OperationContext context) {
        String key = ticker.content() + "_social_media";
        return cache.getOrCompute(key, SocialMediaReport.class, () -> {
            try {
                PromptRunner runner = context.ai()
                        .withLlmByRole(CHEAPEST_ROLE)
                        .withId("generateSocialMediaReport")
                        .withToolObject(newsDataTools);
                String result = streamWithTemplate(runner, "analysts/_BaseAnalyst", Map.of(
                        "tool_names", "get_news",
                        "system_message", promptSocialMediaAnalyst.getContentAsString(StandardCharsets.UTF_8),
                        "ticker", ticker.content().toUpperCase()
                ));
                return new SocialMediaReport(result);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Action(description = "Prepare structured debate briefs from analyst reports")
    public DebateBriefs prepareDebateBriefs(
            Ticker ticker,
            FundamentalsReport fundamentals,
            MarketReport market,
            NewsReport news,
            SocialMediaReport social,
            ActionContext actionContext
    ) {
        if (ticker.content() == null || ticker.content().isBlank()) {
            throw new IllegalArgumentException("Ticker must not be blank");
        }
        if (fundamentals == null || fundamentals.content() == null || fundamentals.content().isBlank()) {
            throw new IllegalArgumentException("Fundamentals report must not be null or blank");
        }
        if (market == null || market.content() == null || market.content().isBlank()) {
            throw new IllegalArgumentException("Market report must not be null or blank");
        }
        if (news == null || news.content() == null || news.content().isBlank()) {
            throw new IllegalArgumentException("News report must not be null or blank");
        }
        if (social == null || social.content() == null || social.content().isBlank()) {
            throw new IllegalArgumentException("Social media report must not be null or blank");
        }
        String key = ticker.content() + "_briefs";
        return cache.getOrCompute(key, DebateBriefs.class, () -> {
            String fb = distill("FUNDAMENTALS", fundamentals.content(), ticker, actionContext);
            String mb = distill("MARKET", market.content(), ticker, actionContext);
            String nb = distill("NEWS", news.content(), ticker, actionContext);
            String sb = distill("SOCIAL MEDIA", social.content(), ticker, actionContext);
            if (fb.isBlank() || mb.isBlank() || nb.isBlank() || sb.isBlank()) {
                throw new IllegalStateException("One or more debate briefs are empty — distillation may have failed");
            }
            return new DebateBriefs(fb, mb, nb, sb);
        });
    }

    private String distill(String reportType, String content, Ticker ticker, ActionContext ctx) {
        PromptRunner runner = ctx.ai()
                .withLlm(new LlmOptions(null, null, CHEAPEST_ROLE, null, null, 4096, null, null, null, null, null))
                .withId("distillBrief_" + reportType.toLowerCase().replace(" ", "_"));
        return streamWithTemplate(runner, "debate/Distiller", Map.of(
                "reportType", reportType,
                "ticker", ticker.content().toUpperCase(),
                "reportContent", content
        ));
    }

    @Action(description = "Debate Investment using Bull and Bear subagents")
    public TraderAgent.InvestmentDebateState debateInvestment(
            TraderAgent.Ticker ticker,
            DebateBriefs briefs,
            ActionContext actionContext
    ) {

        return RepeatUntilBuilder
                .returning(TraderAgent.InvestmentDebateState.class)
                .withMaxIterations(config.maxDebateIterations())
                .repeating(context -> {
                    TraderAgent.InvestmentDebateState last = context.lastAttempt() != null ? context.lastAttempt() : null;

                    List<String> history = last != null ? last.history() : new ArrayList<>();
                    List<String> bullHistory = last != null ? last.bullHistory() : new ArrayList<>();
                    List<String> bearHistory = last != null ? last.bearHistory() : new ArrayList<>();
                    int count = last != null ? last.count() : 0;

                    // Bull turn
                    String bullResponse = cache.getOrCompute(
                            "%s_debate_%d_%s".formatted(ticker.content(), count++, "bull"),
                            String.class,
                            () -> bullAgent.argue(briefs, history, actionContext)
                    );
                    history.add(bullResponse);
                    bullHistory.add(bullResponse);

                    // Bear turn
                    String bearResponse = cache.getOrCompute(
                            "%s_debate_%d_%s".formatted(ticker.content(), count++, "bear"),
                            String.class,
                            () -> bearAgent.argue(briefs, history, actionContext)
                    );
                    history.add(bearResponse);
                    bearHistory.add(bearResponse);

                    // Log similarity for debugging
                    if (bullHistory.size() >= 2) {
                        String prevBull = bullHistory.get(bullHistory.size() - 2);
                        double similarity = computeSimilarity(prevBull, bullResponse);
                        log.info("Debate iteration {} - bull similarity: {:.4f} (threshold: {:.4f})",
                                count, similarity, config.similarityThreshold());
                    }

                    return new TraderAgent.InvestmentDebateState(history, bullHistory, bearHistory, bearResponse, count, briefs, null);
                })
                .until(context -> {
                    if (context.lastAttempt() == null) return false;
                    TraderAgent.InvestmentDebateState state = context.lastAttempt();
                    // Stop at max iterations
                    if (state.count() >= config.maxDebateIterations()) return true;
                    // Stop on convergence: check if last two bull responses are similar enough
                    if (state.bullHistory().size() >= 2) {
                        String prevBull = state.bullHistory().get(state.bullHistory().size() - 2);
                        String currBull = state.bullHistory().get(state.bullHistory().size() - 1);
                        double similarity = computeSimilarity(prevBull, currBull);
                        if (similarity >= config.similarityThreshold()) {
                            log.info("Debate converged at iteration {} (similarity: {:.4f} >= {:.4f})",
                                    state.count(), similarity, config.similarityThreshold());
                            return true;
                        }
                    }
                    return false;
                })
                .build()
                .asSubProcess(actionContext, TraderAgent.InvestmentDebateState.class);
    }

    @Action(description = "Run risk debate to assess risk level for the ticker")
    public RiskAssessment runRiskDebate(
            Ticker ticker,
            DebateBriefs briefs,
            InvestmentDebateState debateState,
            ActionContext actionContext
    ) {
        return riskDebateService.runRiskDebate(ticker, briefs, debateState, actionContext);
    }

    /**
     * Compute Jaccard similarity between two strings using bigrams.
     * Returns a value between 0.0 (no overlap) and 1.0 (identical).
     * Null inputs are treated as empty strings.
     */
    double computeSimilarity(String a, String b) {
        if (a == null || a.isBlank()) {
            return b == null || b.isBlank() ? 1.0 : 0.0;
        }
        if (b == null || b.isBlank()) {
            return 0.0;
        }
        Set<String> bigramsA = bigrams(a);
        Set<String> bigramsB = bigrams(b);
        if (bigramsA.isEmpty() && bigramsB.isEmpty()) return 1.0;
        if (bigramsA.isEmpty() || bigramsB.isEmpty()) return 0.0;

        Set<String> intersection = new HashSet<>(bigramsA);
        intersection.retainAll(bigramsB);

        Set<String> union = new HashSet<>(bigramsA);
        union.addAll(bigramsB);

        return (double) intersection.size() / union.size();
    }

    private Set<String> bigrams(String text) {
        Set<String> result = new HashSet<>();
        String normalized = text.toLowerCase().replaceAll("\\s+", " ").trim();
        for (int i = 0; i < normalized.length() - 1; i++) {
            result.add(normalized.substring(i, i + 2));
        }
        return result;
    }

    /**
     * Human-in-the-loop checkpoint: pauses the agent after the debate completes
     * and before the final investment plan is generated. The user reviews the debate
     * and provides feedback or approval.
     *
     * <p>This is Embabel's native HITL pattern — the process enters a WAITING state,
     * a form is auto-generated from the {@link InvestmentReviewFeedback} record structure,
     * and the agent resumes once the user submits the form.
     *
     * @see <a href="https://embabel.com/docs/states#human-in-the-loop-with-waitfor">Embabel HITL with WaitFor</a>
     */
    @Action(description = "Human review checkpoint — wait for feedback before generating final plan")
    public InvestmentReviewFeedback waitForReview(InvestmentDebateState investmentDebateState, Ticker ticker) {
        return WaitFor.formSubmission(
                "Review the investment debate below and provide feedback, or approve to proceed with the final plan.",
                InvestmentReviewFeedback.class
        );
    }


    @Action(description = "Research Manager to make final investment plan")
    public InvestmentPlan researchManager(
            Ticker ticker,
            InvestmentDebateState investmentDebateState,
            InvestmentReviewFeedback reviewFeedback,
            OperationContext context
    ) {
        String key = ticker.content() + "_research_manager";
        return cache.getOrCompute(key, InvestmentPlan.class, () -> {
            PromptRunner runner = context.ai()
                    .withLlmByRole(BEST_ROLE)
                    .withId("researchManager");

            // Build the model with past memories, debate history, and user feedback
            Map<String, Object> model = new java.util.HashMap<>();
            model.put("past_memory_str", NO_PAST_MEMORIES_FOUND);
            model.put("history", String.join("\n", investmentDebateState.history()));

            // Pass risk assessment if available
            if (investmentDebateState.riskAssessment() != null) {
                RiskAssessment risk = investmentDebateState.riskAssessment();
                model.put("risk_level", risk.level().name());
                model.put("risk_reasoning", risk.reasoning());
            }

            // Pass approval status so the template knows if the human approved
            boolean approved = reviewFeedback != null && reviewFeedback.approved();
            model.put("human_approved", approved);

            // Inject user feedback from HITL (WaitFor pre-execution checkpoint) only when approved.
            // Feedback is additional context for plan generation — it is meaningless when the human
            // has not approved (the LLM should not generate a plan in that case).
            // Sanitize and delimit to prevent prompt injection attacks.
            if (approved && reviewFeedback != null && reviewFeedback.feedback() != null && !reviewFeedback.feedback().isBlank()) {
                model.put("user_feedback", sanitizeForPrompt(reviewFeedback.feedback()));
            } else {
                model.put("user_feedback", null);
            }

            String result = streamWithTemplate(runner, "managers/ResearchManager", model);
            return new InvestmentPlan(result, investmentDebateState);
        });
    }

    /**
     * Generate a research plan for the given ticker.
     *
     * <p>This action generates a high-level research plan using the ResearchManager prompt
     * with {@code human_approved=false}. The plan is presented to the user for review
     * and approval via the {@link #waitForPlanApproval} HITL checkpoint before execution.
     *
     * @param ticker the ticker to research
     * @param context the operation context
     * @return the generated research plan
     */
    @Action(description = "Generate a research plan for the given ticker")
    public ResearchPlan generateResearchPlan(Ticker ticker, OperationContext context) {
        String key = ticker.content() + "_research_plan";
        return cache.getOrCompute(key, ResearchPlan.class, () -> {
            PromptRunner runner = context.ai()
                    .withLlmByRole(BEST_ROLE)
                    .withId("generateResearchPlan");

            Map<String, Object> model = new java.util.HashMap<>();
            model.put("past_memory_str", NO_PAST_MEMORIES_FOUND);
            model.put("history", "");
            // human_approved=false tells the template to generate a plan (not an execution)
            model.put("human_approved", false);
            model.put("user_feedback", null);

            String result = streamWithTemplate(runner, "managers/ResearchManager", model);
            return new ResearchPlan(result);
        });
    }

    /**
     * Human-in-the-loop checkpoint: pauses the agent after the research plan is generated
     * and before the full debate workflow is executed. The user reviews the plan
     * and provides feedback or approval.
     *
     * <p>This is Embabel's native HITL pattern — the process enters a WAITING state,
     * a form is auto-generated from the {@link PlanApproval} record structure,
     * and the agent resumes once the user submits the form.
     *
     * @param researchPlan the generated research plan to review
     * @param ticker the ticker being researched
     * @return the user's approval decision
     */
    @Action(description = "Wait for user to review and approve the research plan before execution")
    public PlanApproval waitForPlanApproval(ResearchPlan researchPlan, Ticker ticker) {
        return WaitFor.formSubmission(
                "Review the research plan below and provide feedback, or approve to execute the full research workflow.",
                PlanApproval.class
        );
    }

    /**
     * Execute the full research workflow after the plan has been approved.
     *
     * <p>Runs the complete pipeline: generate analyst reports → prepare debate briefs →
     * run bull/bear debate → HITL review → generate final investment plan.
     *
     * @param ticker the ticker to research
     * @param planApproval the user's approval decision with optional feedback
     * @param context the operation context
     * @return the final investment plan
     */
    @AchievesGoal(description = "Execute full research workflow after plan approval")
    @Action(description = "Execute the full research workflow after plan approval")
    public InvestmentPlan executeFullResearch(Ticker ticker, PlanApproval planApproval, OperationContext context) {
        // Generate analyst reports
        FundamentalsReport fundamentals = generateFundamentalsReport(ticker, context);
        MarketReport market = generateMarketReport(ticker, context);
        NewsReport news = generateNewsReport(ticker, context);
        SocialMediaReport social = generateSocialMediaReport(ticker, context);

        // Prepare debate briefs
        DebateBriefs briefs = prepareDebateBriefs(ticker, fundamentals, market, news, social, null);

        // Run the debate
        InvestmentDebateState debateState = debateInvestment(ticker, briefs, null);

        // Run risk debate
        RiskAssessment riskAssessment = runRiskDebate(ticker, briefs, debateState, null);
        InvestmentDebateState debateStateWithRisk = new InvestmentDebateState(
                debateState.history(), debateState.bullHistory(), debateState.bearHistory(),
                debateState.currentResponse(), debateState.count(), debateState.briefs(), riskAssessment);

        // HITL review after debate
        InvestmentReviewFeedback reviewFeedback = waitForReview(debateStateWithRisk, ticker);

        // Generate final plan with user feedback from plan approval (if approved)
        String key = ticker.content() + "_research_manager";
        return cache.getOrCompute(key, InvestmentPlan.class, () -> {
            PromptRunner runner = context.ai()
                    .withLlmByRole(BEST_ROLE)
                    .withId("researchManager");

            Map<String, Object> model = new java.util.HashMap<>();
            model.put("past_memory_str", NO_PAST_MEMORIES_FOUND);
            model.put("history", String.join("\n", debateState.history()));

            // Pass risk assessment to the research manager
            if (debateStateWithRisk.riskAssessment() != null) {
                RiskAssessment risk = debateStateWithRisk.riskAssessment();
                model.put("risk_level", risk.level().name());
                model.put("risk_reasoning", risk.reasoning());
            }

            boolean approved = planApproval != null && planApproval.approved();
            model.put("human_approved", approved);

            // Inject user feedback from plan approval HITL checkpoint if approved
            if (approved && planApproval != null && planApproval.feedback() != null && !planApproval.feedback().isBlank()) {
                model.put("user_feedback", sanitizeForPrompt(planApproval.feedback()));
            } else {
                model.put("user_feedback", null);
            }

            String result = streamWithTemplate(runner, "managers/ResearchManager", model);
            return new InvestmentPlan(result, debateState);
        });
    }

    /**
     * Render a Jinja template and stream the LLM response, collecting all chunks into a single String.
     * Falls back to blocking createObject if streaming is not supported.
     * Uses boundedElastic scheduler to avoid event-loop deadlock.
     */
    private String streamWithTemplate(PromptRunner runner, String templateName, Map<String, Object> model) {
        String prompt;
        try {
            prompt = templateRenderer.renderLoadedTemplate(templateName, model);
        } catch (Exception e) {
            throw new RuntimeException("Template rendering failed for [" + templateName + "]", e);
        }
        if (runner.supportsStreaming()) {
            Flux<String> stream = ((StreamingPromptRunner) runner).streaming()
                    .withPrompt(prompt)
                    .generateStream()
                    .subscribeOn(Schedulers.boundedElastic());
            List<String> chunks = stream.collectList()
                    .block(java.time.Duration.ofSeconds(120));
            if (chunks == null || chunks.isEmpty()) {
                throw new IllegalStateException("LLM stream returned no chunks for template [" + templateName + "]");
            }
            return chunks.stream().collect(Collectors.joining());
        }
        // Fallback: blocking call with the same template
        String result = runner.withTemplate(templateName).createObject(String.class, model);
        if (result == null || result.isBlank()) {
            throw new IllegalStateException("LLM returned blank response for template [" + templateName + "]");
        }
        return result;
    }

    /**
     * Parse a Ticker from an LLM response (JSON or plain text).
     */
    private Ticker parseTicker(String response) {
        String content = response.trim();
        // Try to extract from JSON like {"content":"AAPL"}
        int jsonStart = content.indexOf("\"content\"");
        if (jsonStart >= 0) {
            int colon = content.indexOf(':', jsonStart);
            int quoteStart = content.indexOf('"', colon);
            int quoteEnd = content.indexOf('"', quoteStart + 1);
            if (quoteStart >= 0 && quoteEnd > quoteStart) {
                String extracted = content.substring(quoteStart + 1, quoteEnd).trim();
                if (!extracted.isEmpty()) {
                    return new Ticker(extracted, "");
                }
            }
        }
        // Fallback: extract the last quoted string or use the whole content
        int lastQuoteStart = content.lastIndexOf('"');
        if (lastQuoteStart > 0) {
            int prevQuote = content.lastIndexOf('"', lastQuoteStart - 1);
            if (prevQuote >= 0) {
                String extracted = content.substring(prevQuote + 1, lastQuoteStart).trim();
                if (!extracted.isEmpty()) {
                    return new Ticker(extracted, "");
                }
            }
        }
        return new Ticker(content, "");
    }

    /**
     * Sanitize user input before injecting it into an LLM prompt.
     *
     * <p>Prevents prompt injection by:
     * <ol>
     *   <li>Stripping Jinja template syntax ({{ }}, {% %}) that could be evaluated by the template engine</li>
     *   <li>Stripping markdown code fences (```) that LLMs use to ignore instructions</li>
     *   <li>Stripping dangerous control characters (NUL, BEL, BS, etc.) while preserving Unicode</li>
     *   <li>Wrapping the input in XML delimiters to clearly separate it from system instructions</li>
     *   <li>Truncating to a reasonable length to prevent context window abuse</li>
     * </ol>
     *
     * @param input the raw user input
     * @return sanitized input safe for LLM prompt injection
     */
    private String sanitizeForPrompt(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        // Strip Jinja template syntax with dot-all flag to handle multi-line patterns
        String sanitized = input
                .replaceAll("(?s)\\{\\{.*?\\}\\}", "[BLOCKED_TEMPLATE]")
                .replaceAll("(?s)\\{%.*?%\\}", "[BLOCKED_TEMPLATE]")
                // Strip unclosed Jinja delimiters
                .replaceAll("(?s)\\{\\{.*", "[BLOCKED_TEMPLATE]")
                .replaceAll("(?s)\\{%.*", "[BLOCKED_TEMPLATE]")
                // Strip markdown code fences
                .replaceAll("(?s)```.*?", "[BLOCKED_CODE]");

        // Strip only dangerous control characters while preserving all Unicode printables.
        // Keep: printable characters (all Unicode), tab, newline, carriage return.
        // Strip: NUL, BEL, BS, and other control/escape characters.
        StringBuilder sb = new StringBuilder(sanitized.length());
        for (int i = 0; i < sanitized.length(); i++) {
            char c = sanitized.charAt(i);
            if (c == '\t' || c == '\n' || c == '\r') {
                sb.append(c);
            } else if (c >= 0x20 && Character.isISOControl(c) == false) {
                // Printable characters (ASCII and Unicode) — keep
                sb.append(c);
            }
            // else: control characters (NUL, BEL, BS, ESC, etc.) — strip
        }
        sanitized = sb.toString();

        // Truncate to prevent context window abuse (1000 chars is generous for feedback)
        if (sanitized.length() > 1000) {
            sanitized = sanitized.substring(0, 1000) + "...[truncated]";
        }
        // Wrap in XML delimiters to clearly separate from system instructions
        return "<user_feedback>\n" + sanitized + "\n</user_feedback>";
    }
}
