package com.embabel.gekko.agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.ActionContext;
import com.embabel.agent.api.common.Ai;
import com.embabel.agent.api.common.OperationContext;
import com.embabel.agent.api.common.workflow.loop.Feedback;
import com.embabel.agent.api.common.workflow.loop.RepeatUntilBuilder;
import com.embabel.agent.domain.io.UserInput;
import com.embabel.gekko.agent.researchers.BearResearcher;
import com.embabel.gekko.agent.researchers.BullResearcher;
import com.embabel.gekko.config.TraderAgentConfig;
import com.embabel.gekko.tools.FundamentalDataTools;
import com.embabel.gekko.tools.NewsDataTools;
import com.embabel.gekko.util.FileCache;
import com.embabel.gekko.web.TradingHtmxController;
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

    private final FileCache cache;

    private final TraderAgentConfig config;
    private final BullResearcher bullAgent;
    private final BearResearcher bearAgent;

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

    @Action(description = "Convert user input to Ticker object")
    public Ticker tickerFromUserInput(UserInput userInput, Ai ai) {
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

    @Action(description = "Convert form input to Ticker object")
    public Ticker tickerFromForm(TradingHtmxController.TickerForm tickerForm) {
        return new Ticker(tickerForm.getContent());
    }

    @Action(description = "Generate Fundamentals Report")
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

    @Action(description = "Generate Market Report")
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

    @Action(description = "Generate News Report")
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

    @Action(description = "Generate Social Media Report")
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

    @Action(description = "Debate Investment using Bull and Bear subagents")
    public TraderAgent.InvestmentDebateState debateInvestment(
            TraderAgent.Ticker ticker,
            TraderAgent.FundamentalsReport fundamentals,
            TraderAgent.MarketReport market,
            TraderAgent.NewsReport news,
            TraderAgent.SocialMediaReport social,
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
                    String bullResponse = bullAgent.argue(ticker, fundamentals, market, news, social, history, actionContext, count++);
                    history.add(bullResponse);
                    bullHistory.add(bullResponse);

                    // Bear turn
                    String bearResponse = bearAgent.argue(ticker, fundamentals, market, news, social, history, actionContext, count++);
                    history.add(bearResponse);
                    bearHistory.add(bearResponse);

                    return new TraderAgent.InvestmentDebateState(history, bullHistory, bearHistory, bearResponse, count);
                })
                .until(_ -> false)
                .build()
                .asSubProcess(actionContext, TraderAgent.InvestmentDebateState.class);
    }


    @AchievesGoal(description = "We have a result")
    @Action(description = "Research Manager to make final investment plan")
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
