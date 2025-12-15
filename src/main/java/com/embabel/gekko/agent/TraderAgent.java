package com.embabel.gekko.agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.ActionContext;
import com.embabel.agent.api.common.Ai;
import com.embabel.agent.api.common.OperationContext;
import com.embabel.agent.api.common.workflow.loop.Feedback;
import com.embabel.agent.api.common.workflow.loop.RepeatUntilAcceptableBuilder;
import com.embabel.agent.domain.io.UserInput;
import com.embabel.gekko.config.TraderAgentConfig;
import com.embabel.gekko.tools.FundamentalDataTools;
import com.embabel.gekko.tools.NewsDataTools;
import com.embabel.gekko.util.FileCache;
import lombok.RequiredArgsConstructor;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.embabel.common.ai.model.ModelProvider.BEST_ROLE;
import static com.embabel.common.ai.model.ModelProvider.CHEAPEST_ROLE;

@Agent(description = "Trading Agent")
@RegisterReflectionForBinding({
        com.embabel.gekko.agent.TraderAgent.FundamentalsReport.class,
        com.embabel.gekko.agent.TraderAgent.MarketReport.class,
        com.embabel.gekko.agent.TraderAgent.NewsReport.class,
        com.embabel.gekko.agent.TraderAgent.SocialMediaReport.class,
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

    private final FileCache cache = new FileCache("data/llm/cache");

    private final TraderAgentConfig config;

    public interface Report {
        String content();
    }

    public record FundamentalsReport(String content) implements Report {
    }

    public record MarketReport(String content) implements Report {
    }


    public record NewsReport(String content) implements Report {
    }

    public record SocialMediaReport(String content) implements Report {
    }

    public record InvestmentDebateState(List<String> history, List<String> bullHistory, List<String> bearHistory,
                                        String currentResponse,
                                        int count) implements Report {
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
                                           int count) implements Feedback, Report {
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

    @Action
    public Ticker extractTicker(UserInput userInput, Ai ai) {
        String key = userInput.getContent() + "_ticker";
        return cache.getOrCompute(key, Ticker.class, () -> ai
                .withLlmByRole(CHEAPEST_ROLE)
                .withId("extractTicker")
                .creating(Ticker.class)
                .fromPrompt("""
                        Extract ticker from this user input:
                        %s
                        """.formatted(userInput.getContent())));
    }

    @Action
    public FundamentalsReport generateFundamentalsReport(Ticker ticker, OperationContext context) {

        String key = ticker.content() + "_fundamentals";
        return cache.getOrCompute(key, FundamentalsReport.class, () -> {
            try {
                return new FundamentalsReport(context.ai()
                        .withLlmByRole(BEST_ROLE)
                        .withId("generateFundamentalsReport")
                        .withToolObject(fundamentalDataTools)
                        .withTemplate("analysts/_BaseAnalyst")
                        .createObject(String.class, Map.of(
                                "tool_names", "get_fundamentals,get_balance_sheet,get_cashflow,get_income_statement",
                                "system_message", promptFundamentalsAnalyst.getContentAsString(Charset.defaultCharset()),
                                "ticker", ticker.content().toUpperCase()
                        )));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Action
    public MarketReport generateMarketReport(Ticker ticker, OperationContext context) {
        String key = ticker.content() + "_market";
        return cache.getOrCompute(key, MarketReport.class, () -> {
            try {
                return new MarketReport(context.ai()
                        .withLlmByRole(BEST_ROLE)
                        .withId("generateMarketReport")
                        .withTemplate("analysts/_BaseAnalyst").createObject(String.class, Map.of(
                                //"tool_names", "get_stock_data,get_indicators",
                                "system_message", promptMarketAnalyst.getContentAsString(Charset.defaultCharset()),
                                "ticker", ticker.content().toUpperCase()
                        )));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Action
    public NewsReport generateNewsReport(Ticker ticker, OperationContext context) {
        String key = ticker.content() + "_news";
        return cache.getOrCompute(key, NewsReport.class, () -> {
            try {
                return new NewsReport(context.ai()
                        .withLlmByRole(BEST_ROLE)
                        .withId("generateNewsReport")
                        .withToolObject(newsDataTools)
                        .withTemplate("analysts/_BaseAnalyst").createObject(String.class, Map.of(
                                "tool_names", "get_news,get_global_news",
                                "system_message", promptNewsAnalyst.getContentAsString(Charset.defaultCharset()),
                                "ticker", ticker.content().toUpperCase()
                        )));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Action
    public SocialMediaReport generateSocialMediaReport(Ticker ticker, OperationContext context) {
        String key = ticker.content() + "_social_media";
        return cache.getOrCompute(key, SocialMediaReport.class, () -> {
            try {
                return new SocialMediaReport(context.ai()
                        .withLlmByRole(BEST_ROLE)
                        .withId("generateSocialMediaReport")
                        .withToolObject(newsDataTools)
                        .withTemplate("analysts/_BaseAnalyst").createObject(String.class, Map.of(
                                "tool_names", "get_news",
                                "system_message", promptSocialMediaAnalyst.getContentAsString(Charset.defaultCharset()),
                                "ticker", ticker.content().toUpperCase()
                        )));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Action
    public InvestmentDebateState debateInvestment(
            Ticker ticker,
            FundamentalsReport fundamentalsReport,
            MarketReport marketReport,
            NewsReport newsReport,
            SocialMediaReport socialMediaReport,
            ActionContext actionContext) {

        var bearResearcherPromptRunner = actionContext.ai()
                .withLlmByRole(CHEAPEST_ROLE)
                .withId("bearResearcher")
                .withTemplate("researchers/BearResearcher");
        var bullResearcherPromptRunner = actionContext.ai()
                .withLlmByRole(CHEAPEST_ROLE)
                .withId("bullResearcher")
                .withTemplate("researchers/BullResearcher");

        return RepeatUntilAcceptableBuilder
                .returning(InvestmentDebateState.class)
                .withMaxIterations(2)
                .withScoreThreshold(0.9)
                .withFeedbackClass(InvestmentDebateFeedback.class)
                .repeating(context -> {
                    var lastAttempt = context.lastAttempt();
                    int count = lastAttempt != null ? lastAttempt.getFeedback().count + 1 : 0;

                    return cache.getOrCompute(ticker.content() + "_debate_" + count + "_bear", InvestmentDebateState.class, () -> {
                        var feedback = lastAttempt != null ? lastAttempt.getFeedback().currentResponse : "No Bull Argument Yet.";
                        var history = lastAttempt != null ? lastAttempt.getFeedback().history : new ArrayList<String>();
                        var bullHistory = lastAttempt != null ? lastAttempt.getFeedback().bullHistory : new ArrayList<String>();
                        var bearHistory = lastAttempt != null ? lastAttempt.getFeedback().bearHistory : new ArrayList<String>();
                        String currentResponse = "# Bear Analyst\n" + bearResearcherPromptRunner.createObject(String.class, Map.of(
                                "market_research_report", marketReport.content(),
                                "sentiment_report", socialMediaReport.content(),
                                "news_report", newsReport.content(),
                                "fundamentals_report", fundamentalsReport.content(),
                                "history", history.isEmpty() ? "No history yet." : String.join("\n", history),
                                // Last bull currentResponse
                                "current_response", feedback,
                                "past_memory_str", NO_PAST_MEMORIES_FOUND
                        ));
                        history.add(currentResponse);
                        bearHistory.add(currentResponse);
                        return new InvestmentDebateState(history, bullHistory, bearHistory, currentResponse, count);
                    });
                })
                .withEvaluator(context -> {
                            InvestmentDebateState resultToEvaluate = context.getResultToEvaluate();
                            int count = resultToEvaluate.count + 1;
                            return cache.getOrCompute(ticker.content() + "_debate_" + count + "_bull", InvestmentDebateFeedback.class, () -> {
                                var lastBearArgument = resultToEvaluate.currentResponse;
                                var history = resultToEvaluate.history;
                                var bullHistory = resultToEvaluate.bullHistory;
                                var bearHistory = resultToEvaluate.bearHistory;
                                String currentResponse = "# Bull Analyst\n" + bullResearcherPromptRunner.createObject(String.class, Map.of(
                                        "market_research_report", marketReport.content(),
                                        "sentiment_report", socialMediaReport.content(),
                                        "news_report", newsReport.content(),
                                        "fundamentals_report", fundamentalsReport.content(),
                                        "history", String.join("\n", history),
                                        // Last bear currentResponse
                                        "current_response", lastBearArgument,
                                        "past_memory_str", NO_PAST_MEMORIES_FOUND
                                ));
                                history.add(currentResponse);
                                bullHistory.add(currentResponse);
                                return new InvestmentDebateFeedback(history, bullHistory, bearHistory, currentResponse, count);
                            });
                        }
                )
                .build()
                .asSubProcess(actionContext, InvestmentDebateState.class);

    }

    @AchievesGoal(description = "We have a result")
    @Action
    public InvestmentPlan researchManager(Ticker ticker, InvestmentDebateState investmentDebateState, OperationContext context) {
        String key = ticker.content() + "_research_manager";
        return cache.getOrCompute(key, InvestmentPlan.class, () -> new InvestmentPlan(context.ai()
                .withLlmByRole(CHEAPEST_ROLE)
                .withId("researchManager")
                .withTemplate("managers/ResearchManager").createObject(String.class, Map.of(
                        "past_memory_str", NO_PAST_MEMORIES_FOUND,
                        "history", String.join("\n", investmentDebateState.history())
                )), investmentDebateState));

    }
}
