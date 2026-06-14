package com.embabel.gekko.agent;

import com.embabel.agent.test.unit.FakeOperationContext;
import com.embabel.agent.test.unit.FakePromptRunner;
import com.embabel.agent.test.unit.LlmInvocation;
import com.embabel.common.ai.model.ModelProvider;
import com.embabel.gekko.agent.TraderAgent.Ticker;
import com.embabel.gekko.config.TraderAgentConfig;
import com.embabel.gekko.domain.Analysts.FundamentalsReport;
import com.embabel.gekko.domain.Analysts.MarketReport;
import com.embabel.gekko.domain.Analysts.NewsReport;
import com.embabel.gekko.domain.Analysts.SocialMediaReport;
import com.embabel.gekko.tools.FundamentalDataTools;
import com.embabel.gekko.tools.NewsDataTools;
import com.embabel.common.textio.template.TemplateRenderer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TraderAgent LLM-calling actions using FakeOperationContext + FakePromptRunner.
 * Verifies LLM role selection, interaction IDs, prompt content, and tool attachment.
 */
class TraderAgentLLMTest {

    private FakeOperationContext ctx;
    private FakePromptRunner promptRunner;
    private TraderAgent agent;

    @BeforeEach
    void setUp() {
        ctx = FakeOperationContext.create();
        promptRunner = ctx.getPromptRunner();

        TemplateRenderer renderer = new TemplateRenderer() {
            @Override public String load(String name) { return "template"; }
            @Override public String renderLoadedTemplate(String name, Map<String, ?> model) { return "test prompt"; }
            @Override public String renderLiteralTemplate(String template, Map<String, ?> model) { return "test prompt"; }
        };

        TraderAgentConfig config = new TraderAgentConfig(
                null, null, 4, null, null, null,
                "/tmp", 0.8, 5
        );

        agent = new TraderAgent(
                new com.embabel.gekko.util.FileCache(),
                null,
                new FundamentalDataTools(null),
                new NewsDataTools(null),
                config, null, null, null, renderer
        );
    }

    // --- tickerFromUserInput tests ---

    @Test
    void tickerFromUserInput_usesCheapestRoleAndCorrectId() {
        // Arrange
        ctx.expectResponse(new Ticker("AAPL", ""));

        // Act
        Ticker result = agent.tickerFromUserInput(
                new com.embabel.agent.domain.io.UserInput("Tell me about Apple stock"),
                ctx
        );

        // Assert
        assertEquals("AAPL", result.content());
        List<LlmInvocation> invocations = promptRunner.getLlmInvocations();
        assertEquals(1, invocations.size());
        var inv = invocations.get(0);
        assertEquals("extractTicker", inv.getInteraction().getId());
    }

    @Test
    void tickerFromUserInput_promptContainsUserInput() {
        // Arrange
        ctx.expectResponse(new Ticker("NVDA", ""));

        // Act
        agent.tickerFromUserInput(
                new com.embabel.agent.domain.io.UserInput("I want to invest in NVIDIA"),
                ctx
        );

        // Assert
        String prompt = promptRunner.getLlmInvocations().get(0).getPrompt();
        assertTrue(prompt.contains("NVIDIA"));
    }

    @Test
    void tickerFromUserInput_blankInputThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                agent.tickerFromUserInput(new com.embabel.agent.domain.io.UserInput(""), ctx)
        );
    }

    // --- generateFundamentalsReport tests ---

    @Test
    void generateFundamentalsReport_usesCorrectId() {
        // Arrange
        Ticker ticker = new Ticker("AAPL", "");
        ctx.expectResponse(new FundamentalsReport("Strong revenue growth."));

        // Act
        FundamentalsReport result = agent.generateFundamentalsReport(ticker, ctx);

        // Assert
        assertNotNull(result);
        List<LlmInvocation> invocations = promptRunner.getLlmInvocations();
        assertEquals(1, invocations.size());
        var inv = invocations.get(0);
        assertEquals("generateFundamentalsReport", inv.getInteraction().getId());
    }

    @Test
    void generateFundamentalsReport_promptContainsTickerAndToolNames() {
        // Arrange
        Ticker ticker = new Ticker("TSLA", "");
        ctx.expectResponse(new FundamentalsReport("Revenue up."));

        // Act
        agent.generateFundamentalsReport(ticker, ctx);

        // Assert
        String prompt = promptRunner.getLlmInvocations().get(0).getPrompt();
        assertTrue(prompt.contains("TSLA"));
        assertTrue(prompt.contains("get_fundamentals"));
        assertTrue(prompt.contains("get_balance_sheet"));
        assertTrue(prompt.contains("get_cashflow"));
        assertTrue(prompt.contains("get_income_statement"));
    }

    // --- generateMarketReport tests ---

    @Test
    void generateMarketReport_usesCorrectId() {
        // Arrange
        Ticker ticker = new Ticker("GOOGL", "");
        ctx.expectResponse(new MarketReport("Bullish breakout."));

        // Act
        MarketReport result = agent.generateMarketReport(ticker, ctx);

        // Assert
        assertNotNull(result);
        List<LlmInvocation> invocations = promptRunner.getLlmInvocations();
        assertEquals(1, invocations.size());
        var inv = invocations.get(0);
        assertEquals("generateMarketReport", inv.getInteraction().getId());
    }

    @Test
    void generateMarketReport_promptContainsCorrectToolNames() {
        // Arrange
        ctx.expectResponse(new MarketReport("Chart pattern."));

        // Act
        agent.generateMarketReport(new Ticker("MSFT", ""), ctx);

        // Assert
        String prompt = promptRunner.getLlmInvocations().get(0).getPrompt();
        assertTrue(prompt.contains("get_stock_data"));
        assertTrue(prompt.contains("get_indicators"));
    }

    // --- generateNewsReport tests ---

    @Test
    void generateNewsReport_usesCorrectId() {
        // Arrange
        Ticker ticker = new Ticker("AMZN", "");
        ctx.expectResponse(new NewsReport("Positive product news."));

        // Act
        NewsReport result = agent.generateNewsReport(ticker, ctx);

        // Assert
        assertNotNull(result);
        List<LlmInvocation> invocations = promptRunner.getLlmInvocations();
        assertEquals(1, invocations.size());
        var inv = invocations.get(0);
        assertEquals("generateNewsReport", inv.getInteraction().getId());
    }

    // --- generateSocialMediaReport tests ---

    @Test
    void generateSocialMediaReport_usesCorrectId() {
        // Arrange
        Ticker ticker = new Ticker("META", "");
        ctx.expectResponse(new SocialMediaReport("Positive sentiment."));

        // Act
        SocialMediaReport result = agent.generateSocialMediaReport(ticker, ctx);

        // Assert
        assertNotNull(result);
        List<LlmInvocation> invocations = promptRunner.getLlmInvocations();
        assertEquals(1, invocations.size());
        var inv = invocations.get(0);
        assertEquals("generateSocialMediaReport", inv.getInteraction().getId());
    }
}
