package com.embabel.gekko.agent.researchers;

import com.embabel.agent.api.common.ActionContext;
import com.embabel.gekko.agent.TraderAgent;
import com.embabel.gekko.domain.Analysts.FundamentalsReport;
import com.embabel.gekko.domain.Analysts.MarketReport;
import com.embabel.gekko.domain.Analysts.NewsReport;
import com.embabel.gekko.domain.Analysts.SocialMediaReport;
import com.embabel.gekko.util.FileCache;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.embabel.common.ai.model.ModelProvider.CHEAPEST_ROLE;

@Component
@RequiredArgsConstructor
public class BearResearcher {

    private final FileCache cache;

    public String argue(
            FundamentalsReport fundamentals,
            MarketReport market,
            NewsReport news,
            SocialMediaReport social,
            List<String> history,
            ActionContext actionContext
    ) {
        String previousResponse = history.isEmpty() ? "No argument yet." : history.getLast();

        return "# Bear Analyst\n" + actionContext.ai()
                .withLlmByRole(CHEAPEST_ROLE)
                .withId("bearResearcher")
                .withTemplate("researchers/BearResearcher")
                .createObject(String.class, Map.of(
                        "market_research_report", market.content(),
                        "sentiment_report", social.content(),
                        "news_report", news.content(),
                        "fundamentals_report", fundamentals.content(),
                        "history", history.isEmpty() ? "No history yet." : String.join("\n", history),
                        "current_response", previousResponse,
                        "past_memory_str", TraderAgent.NO_PAST_MEMORIES_FOUND
                ));
    }
}
