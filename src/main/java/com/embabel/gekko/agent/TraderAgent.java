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
import com.embabel.gekko.tools.FundamentalDataTools;
import com.embabel.gekko.tools.NewsDataTools;
import com.embabel.gekko.util.FileCache;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

@Agent(description = "Trading Agent")
@RequiredArgsConstructor
public class TraderAgent {
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

    public record InvestmentDebateState(String history, String bullHistory, String bearHistory, String currentResponse,
                                        int count) implements Report {
        @Override
        public String content() {
            return currentResponse;
        }
    }

    public record InvestmentDebateFeedback(String history, String bullHistory, String bearHistory,
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
        return cache.getOrCompute(key, Ticker.class, () -> ai.withAutoLlm().withId("extractTicker")
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
                return context.ai().withAutoLlm().withId("generateFundamentalsReport")
                        .withToolObject(fundamentalDataTools)
                        .withTemplate("analysts/_BaseAnalyst").createObject(FundamentalsReport.class, Map.of(
                                "tool_names", "get_fundamentals,get_balance_sheet,get_cashflow,get_income_statement",
                                "system_message", promptFundamentalsAnalyst.getContentAsString(Charset.defaultCharset()),
                                "ticker", ticker.content().toUpperCase()
                        ));
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
                return context.ai().withAutoLlm().withId("generateMarketReport")
                        .withTemplate("analysts/_BaseAnalyst").createObject(MarketReport.class, Map.of(
                                //"tool_names", "get_stock_data,get_indicators",
                                "system_message", promptMarketAnalyst.getContentAsString(Charset.defaultCharset()),
                                "ticker", ticker.content().toUpperCase()
                        ));
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
                return new NewsReport(context.ai().withAutoLlm().withId("generateNewsReport")
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
                return new SocialMediaReport(context.ai().withAutoLlm().withId("generateSocialMediaReport")
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

    @AchievesGoal(description = "We have a debate")
    @Action
    public InvestmentDebateState debateInvestment(
            Ticker ticker,
            FundamentalsReport fundamentalsReport,
            MarketReport marketReport,
            NewsReport newsReport,
            SocialMediaReport socialMediaReport,
            ActionContext actionContext) {

        var bearResearcherPromptRunner = actionContext.ai().withAutoLlm().withId("bearResearcher")
                .withTemplate("researchers/BearResearcher.jinja");
        var bullResearcherPromptRunner = actionContext.ai().withAutoLlm().withId("bullResearcher")
                .withTemplate("researchers/BullResearcher.jinja");


        return RepeatUntilAcceptableBuilder
                .returning(InvestmentDebateState.class)
                .withMaxIterations(1)
                .withScoreThreshold(0.9)
                .withFeedbackClass(InvestmentDebateFeedback.class)
                .repeating(context -> {
                    var lastAttempt = context.lastAttempt();
                    int count = lastAttempt != null ? lastAttempt.getFeedback().count + 1 : 0;

                    return cache.getOrCompute(ticker.content() + "_debate_" + count + "_bear", InvestmentDebateState.class, () -> {
                        var feedback = lastAttempt != null ? "Bull Analyst: " + lastAttempt.getFeedback().currentResponse : "";
                        var history = lastAttempt != null ? lastAttempt.getFeedback().history : "";
                        var bullHistory = lastAttempt != null ? lastAttempt.getFeedback().bullHistory : "";
                        var bearHistory = lastAttempt != null ? lastAttempt.getFeedback().bearHistory : "";
                        String argument = "Bear Analyst: " + bearResearcherPromptRunner.createObject(String.class, Map.of(
                                "market_research_report", marketReport.content(),
                                "sentiment_report", socialMediaReport.content(),
                                "news_report", newsReport.content(),
                                "fundamentals_report", fundamentalsReport.content(),
                                "history", history,
                                // Last bull argument
                                "current_response", feedback,
                                "past_memory_str", "past_memory_str"
                        ));
                        return new InvestmentDebateState(history + "\n" + argument, bullHistory, bearHistory + "\n" + argument, argument, count);
                    });
                })
                .withEvaluator(context -> {
                            InvestmentDebateState resultToEvaluate = context.getResultToEvaluate();
                            int count = resultToEvaluate.count + 1;
                            return cache.getOrCompute(ticker.content() + "_debate_" + count + "_bull", InvestmentDebateFeedback.class, () -> {
                                String lastBearArgument = resultToEvaluate.currentResponse;
                                String history = resultToEvaluate.history;
                                String bullHistory = resultToEvaluate.bullHistory;
                                String bearHistory = resultToEvaluate.bearHistory;
                                String argument = "Bull Analyst: " + bullResearcherPromptRunner.createObject(String.class, Map.of(
                                        "market_research_report", marketReport.content(),
                                        "sentiment_report", socialMediaReport.content(),
                                        "news_report", newsReport.content(),
                                        "fundamentals_report", fundamentalsReport.content(),
                                        "history", history,
                                        // Last bear argument
                                        "current_response", lastBearArgument,
                                        "past_memory_str", "past_memory_str"
                                ));
                                return new InvestmentDebateFeedback(history + "\n" + argument, bullHistory + "\n" + argument, bearHistory, argument, count);
                            });
                        }
                )
                .build()
                .asSubProcess(actionContext, InvestmentDebateState.class);

    }
}
