package com.embabel.template.agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.annotation.WaitFor;
import com.embabel.agent.api.common.Ai;
import com.embabel.agent.api.common.OperationContext;
import com.embabel.agent.domain.io.UserInput;
import com.embabel.template.tools.FundamentalTools;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

@Agent(description = "Trading Agent")
@RequiredArgsConstructor
public class TraderAgent {
    @Value("classpath:prompts/analysts/FundamentalsAnalyst.txt")
    private Resource promptFundamentalsAnalyst;
    private final FundamentalTools fundamentalTools;

    @Value("classpath:prompts/analysts/MarketAnalyst.txt")
    private Resource promptMarketAnalyst;

    public record Topics(
            List<String> topics
    ) {
    }

    public interface Report {
    }

    public record FundamentalsReport(
            String content
    ) implements Report {
    }

    public record MarketReport(
            String content
    ) implements Report {
    }

    public record Ticker(
            String content
    ) {
        public Ticker() {
            this("SPY");
        }
    }

    @Action(cost = 100.0)
    Ticker askForTicker(OperationContext context) {
        return WaitFor.formSubmission(
                "Enter the ticker symbol to analyze.",
                Ticker.class);
    }

    @Action
    public Ticker extractTicker(UserInput userInput, Ai ai) {
        return ai.withAutoLlm()
                .creating(Ticker.class)
                .fromPrompt("""
                        Extract ticker from this user input:
                        %s
                        """.formatted(userInput.getContent()));
    }

    @Action
    public FundamentalsReport generateFundamentalsReport(Ticker ticker, OperationContext context) throws IOException {
        return context.ai().withAutoLlm().withToolObject(fundamentalTools)
                .withTemplate("analysts/_BaseAnalyst").createObject(FundamentalsReport.class, Map.of(
                        "tool_names", "get_fundamentals,get_balance_sheet,get_cashflow,get_income_statement",
                        "system_message", promptFundamentalsAnalyst.getContentAsString(Charset.defaultCharset()),
                        "ticker", ticker.content().toUpperCase()
                ));

    }

    @Action
    public MarketReport generateMarketReport(Ticker ticker, OperationContext context) throws IOException {
        return context.ai().withAutoLlm()
                .withTemplate("analysts/_BaseAnalyst").createObject(MarketReport.class, Map.of(
                        "tool_names", "get_stock_data,get_indicators",
                        "system_message", promptMarketAnalyst.getContentAsString(Charset.defaultCharset()),
                        "ticker", ticker.content().toUpperCase()
                ));

    }

    @Action
    @AchievesGoal(description = "the thing")
    public String bearResearch(FundamentalsReport fundamentalsReport, MarketReport marketReport,
                               OperationContext context) throws IOException {
        return context.ai().withAutoLlm()
                .withTemplate("researchers/BearResearcher.jinja").createObject(String.class, Map.of(
                        "market_research_report", marketReport.content(),
                        "sentiment_report", "sentiment_report",
                        "news_report", "news_report",
                        "fundamentals_report", fundamentalsReport.content(),
                        "history", "history",
                        "current_response", "current_response",
                        "past_memory_str", "past_memory_str"
                ));

    }

}
