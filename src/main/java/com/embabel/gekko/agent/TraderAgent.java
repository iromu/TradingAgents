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
import com.embabel.gekko.tools.NewsDataTools;
import com.embabel.gekko.util.FileCache;
import com.embabel.gekko.web.TradingHtmxController;
import lombok.RequiredArgsConstructor;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
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
        com.embabel.gekko.agent.TraderAgent.Ticker.class
})
@RequiredArgsConstructor
public class TraderAgent {
    public static final String NO_PAST_MEMORIES_FOUND = "No past memories found.";
    @Value("classpath:prompts/analysts/FundamentalsAnalyst.txt")
    private Resource promptFundamentalsAnalyst;
    private final FundamentalDataTools fundamentalDataTools;
    private final NewsDataTools newsDataTools;

    @Value("classpath:prompts/analysts/MarketAnalyst.txt")
    private Resource promptMarketAnalyst;

    @Value("classpath:prompts/analysts/NewsAnalyst.txt")
    private Resource promptNewsAnalyst;

    @Value("classpath:prompts/analysts/SocialMediaAnalyst.txt")
    private Resource promptSocialMediaAnalyst;

    private final FileCache cache;

    private final TraderAgentConfig config;
    private final BullResearcher bullAgent;
    private final BearResearcher bearAgent;
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
                                        DebateBriefs briefs) implements Report {
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
                                           DebateBriefs briefs) implements Feedback, Report {
        @Override
        public double getScore() {
            return 0;
        }

        @Override
        public String content() {
            return currentResponse;
        }
    }

    public record Ticker(String content) {
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
                return parseTicker(result);
            } else {
                return runner.creating(Ticker.class).fromPrompt(prompt);
            }
        });
    }

    @Action(description = "Convert form input to Ticker object")
    public Ticker tickerFromForm(TradingHtmxController.TickerForm tickerForm) {
        String content = tickerForm.getContent();
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Ticker must not be blank");
        }
        return new Ticker(content);
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
                        //"tool_names", "get_stock_data,get_indicators",
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
                .withMaxIterations(2) // 2 rounds of Bull→Bear
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

                    return new TraderAgent.InvestmentDebateState(history, bullHistory, bearHistory, bearResponse, count, briefs);
                })
                .until(_ -> false)
                .build()
                .asSubProcess(actionContext, TraderAgent.InvestmentDebateState.class);
    }


    @AchievesGoal(description = "We have a result")
    @Action(description = "Research Manager to make final investment plan")
    public InvestmentPlan researchManager(Ticker ticker, InvestmentDebateState investmentDebateState, OperationContext context) {
        String key = ticker.content() + "_research_manager";
        return cache.getOrCompute(key, InvestmentPlan.class, () -> {
            PromptRunner runner = context.ai()
                    .withLlmByRole(BEST_ROLE)
                    .withId("researchManager");
            String result = streamWithTemplate(runner, "managers/ResearchManager", Map.of(
                    "past_memory_str", NO_PAST_MEMORIES_FOUND,
                    "history", String.join("\n", investmentDebateState.history())
            ));
            return new InvestmentPlan(result, investmentDebateState);
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
        return runner.withTemplate(templateName).createObject(String.class, model);
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
                    return new Ticker(extracted);
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
                    return new Ticker(extracted);
                }
            }
        }
        return new Ticker(content);
    }
}
