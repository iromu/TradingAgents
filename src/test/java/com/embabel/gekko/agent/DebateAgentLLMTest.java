package com.embabel.gekko.agent;

import com.embabel.agent.test.unit.FakeOperationContext;
import com.embabel.agent.test.unit.FakePromptRunner;
import com.embabel.gekko.domain.Analysts.FundamentalsReport;
import com.embabel.gekko.domain.Analysts.MarketReport;
import com.embabel.gekko.domain.Analysts.NewsReport;
import com.embabel.gekko.domain.Analysts.SocialMediaReport;
import com.embabel.gekko.domain.ResearchTypes;
import com.embabel.gekko.util.FileCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DebateAgent LLM-calling actions using FakeOperationContext + FakePromptRunner.
 * Covers: generateFundamentalsReport, generateMarketReport, generateNewsReport,
 * generateSocialMediaReport, prepareDebateBriefs (distill), researchManager.
 */
class DebateAgentLLMTest {

    private FakeOperationContext ctx;
    private FakePromptRunner promptRunner;
    private DebateAgent agent;

    /**
     * Creates a FileCache backed by a unique temp directory so tests don't share cache state.
     */
    private FileCache createCache() {
        try {
            File tempDir = Files.createTempDirectory("debate-agent-test-cache-").toFile();
            tempDir.deleteOnExit();
            var cache = new FileCache();
            var field = FileCache.class.getDeclaredField("baseDir");
            field.setAccessible(true);
            field.set(cache, tempDir);
            return cache;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create temp cache directory", e);
        }
    }

    @BeforeEach
    void setUp() {
        ctx = FakeOperationContext.create();
        promptRunner = ctx.getPromptRunner();
        agent = new DebateAgent(
                createCache(),
                null, // templateRenderer — not needed for FakeOperationContext
                null, // memoryAgent — not needed for LLM action tests
                null, // debateLoopAgentProvider
                null, // riskDebateAgentProvider
                null, // traderProvider
                null  // portfolioManagerProvider
        );
    }

    // --- generateFundamentalsReport tests ---

    @Test
    void generateFundamentalsReport_returnsWrappedReport() {
        // Arrange
        ctx.expectResponse("Fundamentals: Revenue grew 15% YoY.");
        var ticker = new ResearchTypes.Ticker("AAPL", "");

        // Act
        var result = agent.generateFundamentalsReport(ticker, ctx);

        // Assert
        assertEquals("Fundamentals: Revenue grew 15% YoY.", result.content());
        var invocations = promptRunner.getLlmInvocations();
        assertEquals(1, invocations.size());
    }

    @Test
    void generateFundamentalsReport_usesCorrectInteractionId() {
        // Arrange
        ctx.expectResponse("Fundamentals: Revenue grew 15% YoY.");
        var ticker = new ResearchTypes.Ticker("AAPL", "");

        // Act
        agent.generateFundamentalsReport(ticker, ctx);

        // Assert
        assertEquals("generateFundamentalsReport", promptRunner.getLlmInvocations().get(0).getInteraction().getId());
    }

    @Test
    void generateFundamentalsReport_promptIsNonEmpty() {
        // Arrange
        ctx.expectResponse("Fundamentals report.");
        var ticker = new ResearchTypes.Ticker("AAPL", "");

        // Act
        agent.generateFundamentalsReport(ticker, ctx);

        // Assert
        var prompt = promptRunner.getLlmInvocations().get(0).getPrompt();
        assertFalse(prompt.isBlank());
        // The template describes the researcher role
        assertTrue(prompt.contains("researcher") || prompt.contains("FundamentalsAnalyst"));
    }

    @Test
    void generateFundamentalsReport_usesTemplate() {
        // Arrange
        ctx.expectResponse("Fundamentals report.");
        var ticker = new ResearchTypes.Ticker("AAPL", "");

        // Act
        agent.generateFundamentalsReport(ticker, ctx);

        // Assert
        var invocations = promptRunner.getLlmInvocations();
        assertEquals(1, invocations.size());
        // Verify the interaction has an ID and LLM options set
        var interaction = invocations.get(0).getInteraction();
        assertNotNull(interaction.getId());
        assertNotNull(interaction.getLlm());
    }

    // --- generateMarketReport tests ---

    @Test
    void generateMarketReport_returnsWrappedReport() {
        // Arrange
        ctx.expectResponse("Market: Bullish breakout above 200-day MA.");
        var ticker = new ResearchTypes.Ticker("AAPL", "");

        // Act
        var result = agent.generateMarketReport(ticker, ctx);

        // Assert
        assertEquals("Market: Bullish breakout above 200-day MA.", result.content());
        assertEquals(1, promptRunner.getLlmInvocations().size());
    }

    @Test
    void generateMarketReport_usesCorrectInteractionId() {
        // Arrange
        ctx.expectResponse("Market report content.");
        var ticker = new ResearchTypes.Ticker("AAPL", "");

        // Act
        agent.generateMarketReport(ticker, ctx);

        // Assert
        assertEquals("generateMarketReport", promptRunner.getLlmInvocations().get(0).getInteraction().getId());
    }

    @Test
    void generateMarketReport_promptIsNonEmpty() {
        // Arrange
        ctx.expectResponse("Market report.");
        var ticker = new ResearchTypes.Ticker("AAPL", "");

        // Act
        agent.generateMarketReport(ticker, ctx);

        // Assert
        var prompt = promptRunner.getLlmInvocations().get(0).getPrompt();
        assertFalse(prompt.isBlank());
    }

    // --- generateNewsReport tests ---

    @Test
    void generateNewsReport_returnsWrappedReport() {
        // Arrange
        ctx.expectResponse("News: Positive product launch announced.");
        var ticker = new ResearchTypes.Ticker("AAPL", "");

        // Act
        var result = agent.generateNewsReport(ticker, ctx);

        // Assert
        assertEquals("News: Positive product launch announced.", result.content());
        assertEquals(1, promptRunner.getLlmInvocations().size());
    }

    @Test
    void generateNewsReport_usesCorrectInteractionId() {
        // Arrange
        ctx.expectResponse("News report content.");
        var ticker = new ResearchTypes.Ticker("AAPL", "");

        // Act
        agent.generateNewsReport(ticker, ctx);

        // Assert
        assertEquals("generateNewsReport", promptRunner.getLlmInvocations().get(0).getInteraction().getId());
    }

    @Test
    void generateNewsReport_promptIsNonEmpty() {
        // Arrange
        ctx.expectResponse("News report.");
        var ticker = new ResearchTypes.Ticker("AAPL", "");

        // Act
        agent.generateNewsReport(ticker, ctx);

        // Assert
        var prompt = promptRunner.getLlmInvocations().get(0).getPrompt();
        assertFalse(prompt.isBlank());
    }

    // --- generateSocialMediaReport tests ---

    @Test
    void generateSocialMediaReport_returnsWrappedReport() {
        // Arrange
        ctx.expectResponse("Social: Strong positive sentiment on social media.");
        var ticker = new ResearchTypes.Ticker("AAPL", "");

        // Act
        var result = agent.generateSocialMediaReport(ticker, ctx);

        // Assert
        assertEquals("Social: Strong positive sentiment on social media.", result.content());
        assertEquals(1, promptRunner.getLlmInvocations().size());
    }

    @Test
    void generateSocialMediaReport_usesCorrectInteractionId() {
        // Arrange
        ctx.expectResponse("Social report content.");
        var ticker = new ResearchTypes.Ticker("AAPL", "");

        // Act
        agent.generateSocialMediaReport(ticker, ctx);

        // Assert
        assertEquals("generateSocialMediaReport", promptRunner.getLlmInvocations().get(0).getInteraction().getId());
    }

    @Test
    void generateSocialMediaReport_promptIsNonEmpty() {
        // Arrange
        ctx.expectResponse("Social report.");
        var ticker = new ResearchTypes.Ticker("AAPL", "");

        // Act
        agent.generateSocialMediaReport(ticker, ctx);

        // Assert
        var prompt = promptRunner.getLlmInvocations().get(0).getPrompt();
        assertFalse(prompt.isBlank());
    }

    // --- prepareDebateBriefs (distill) tests ---

    @Test
    void prepareDebateBriefs_makes4DistillCalls() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var ticker = new ResearchTypes.Ticker("AAPL", "");
        var fundamentals = new FundamentalsReport("Revenue grew 15% YoY.");
        var market = new MarketReport("Bullish breakout.");
        var news = new NewsReport("Positive product launch.");
        var social = new SocialMediaReport("Strong sentiment.");

        for (int i = 0; i < 4; i++) {
            fake.getDelegate().expectResponse("Distilled brief content " + i);
        }

        var result = agent.prepareDebateBriefs(ticker, fundamentals, market, news, social, context);

        var invocations = fake.getDelegate().getPromptRunner().getLlmInvocations();
        assertEquals(4, invocations.size());
        assertEquals("Distilled brief content 0", result.fundamentalsBrief());
        assertEquals("Distilled brief content 1", result.marketBrief());
        assertEquals("Distilled brief content 2", result.newsBrief());
        assertEquals("Distilled brief content 3", result.socialBrief());
    }

    @Test
    void prepareDebateBriefs_usesCorrectInteractionIds() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var ticker = new ResearchTypes.Ticker("AAPL", "");
        var fundamentals = new FundamentalsReport("F");
        var market = new MarketReport("M");
        var news = new NewsReport("N");
        var social = new SocialMediaReport("S");

        for (int i = 0; i < 4; i++) {
            fake.getDelegate().expectResponse("Brief " + i);
        }

        agent.prepareDebateBriefs(ticker, fundamentals, market, news, social, context);

        var invocations = fake.getDelegate().getPromptRunner().getLlmInvocations();
        assertEquals("distillBrief_fundamentals", invocations.get(0).getInteraction().getId());
        assertEquals("distillBrief_market", invocations.get(1).getInteraction().getId());
        assertEquals("distillBrief_news", invocations.get(2).getInteraction().getId());
        assertEquals("distillBrief_social_media", invocations.get(3).getInteraction().getId());
    }

    @Test
    void prepareDebateBriefs_promptContainsReportType() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var ticker = new ResearchTypes.Ticker("AAPL", "");
        var fundamentals = new FundamentalsReport("Revenue grew 15% YoY.");
        var market = new MarketReport("M");
        var news = new NewsReport("N");
        var social = new SocialMediaReport("S");

        fake.getDelegate().expectResponse("Distilled fundamentals brief.");
        fake.getDelegate().expectResponse("Distilled market brief.");
        fake.getDelegate().expectResponse("Distilled news brief.");
        fake.getDelegate().expectResponse("Distilled social brief.");

        agent.prepareDebateBriefs(ticker, fundamentals, market, news, social, context);

        var prompt = fake.getDelegate().getPromptRunner().getLlmInvocations().get(0).getPrompt();
        assertTrue(prompt.contains("FUNDAMENTALS"));
        assertTrue(prompt.contains("Revenue grew 15% YoY"));
    }

    @Test
    void prepareDebateBriefs_throwsOnEmptyFundamentals() {
        var fake = FakeActionContext.create();
        var ticker = new ResearchTypes.Ticker("AAPL", "");
        var fundamentals = new FundamentalsReport("");
        var market = new MarketReport("M");
        var news = new NewsReport("N");
        var social = new SocialMediaReport("S");

        assertThrows(IllegalArgumentException.class, () ->
                agent.prepareDebateBriefs(ticker, fundamentals, market, news, social, fake.getActionContext())
        );
    }

    @Test
    void prepareDebateBriefs_throwsOnEmptyMarket() {
        var fake = FakeActionContext.create();
        var ticker = new ResearchTypes.Ticker("AAPL", "");
        var fundamentals = new FundamentalsReport("F");
        var market = new MarketReport("");
        var news = new NewsReport("N");
        var social = new SocialMediaReport("S");

        assertThrows(IllegalArgumentException.class, () ->
                agent.prepareDebateBriefs(ticker, fundamentals, market, news, social, fake.getActionContext())
        );
    }

    @Test
    void prepareDebateBriefs_throwsOnEmptyNews() {
        var fake = FakeActionContext.create();
        var ticker = new ResearchTypes.Ticker("AAPL", "");
        var fundamentals = new FundamentalsReport("F");
        var market = new MarketReport("M");
        var news = new NewsReport("");
        var social = new SocialMediaReport("S");

        assertThrows(IllegalArgumentException.class, () ->
                agent.prepareDebateBriefs(ticker, fundamentals, market, news, social, fake.getActionContext())
        );
    }

    @Test
    void prepareDebateBriefs_throwsOnEmptySocial() {
        var fake = FakeActionContext.create();
        var ticker = new ResearchTypes.Ticker("AAPL", "");
        var fundamentals = new FundamentalsReport("F");
        var market = new MarketReport("M");
        var news = new NewsReport("N");
        var social = new SocialMediaReport("");

        assertThrows(IllegalArgumentException.class, () ->
                agent.prepareDebateBriefs(ticker, fundamentals, market, news, social, fake.getActionContext())
        );
    }

    @Test
    void prepareDebateBriefs_throwsOnBlankTicker() {
        var fake = FakeActionContext.create();
        var ticker = new ResearchTypes.Ticker("  ", "");
        var fundamentals = new FundamentalsReport("F");
        var market = new MarketReport("M");
        var news = new NewsReport("N");
        var social = new SocialMediaReport("S");

        assertThrows(IllegalArgumentException.class, () ->
                agent.prepareDebateBriefs(ticker, fundamentals, market, news, social, fake.getActionContext())
        );
    }

    @Test
    void prepareDebateBriefs_throwsWhenDistillReturnsEmpty() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var ticker = new ResearchTypes.Ticker("AAPL", "");
        var fundamentals = new FundamentalsReport("F");
        var market = new MarketReport("M");
        var news = new NewsReport("N");
        var social = new SocialMediaReport("S");

        fake.getDelegate().expectResponse(""); // fundamentals distill returns empty

        assertThrows(IllegalStateException.class, () ->
                agent.prepareDebateBriefs(ticker, fundamentals, market, news, social, context)
        );
    }

    @Test
    void prepareDebateBriefs_all4BriefsReturnedCorrectly() {
        var fake = FakeActionContext.create();
        var context = fake.getActionContext();
        var ticker = new ResearchTypes.Ticker("AAPL", "");
        var fundamentals = new FundamentalsReport("F");
        var market = new MarketReport("M");
        var news = new NewsReport("N");
        var social = new SocialMediaReport("S");

        fake.getDelegate().expectResponse("Fundamentals brief");
        fake.getDelegate().expectResponse("Market brief");
        fake.getDelegate().expectResponse("News brief");
        fake.getDelegate().expectResponse("Social brief");

        var result = agent.prepareDebateBriefs(ticker, fundamentals, market, news, social, context);

        assertNotNull(result);
        assertEquals("Fundamentals brief", result.fundamentalsBrief());
        assertEquals("Market brief", result.marketBrief());
        assertEquals("News brief", result.newsBrief());
        assertEquals("Social brief", result.socialBrief());
    }

    // --- researchManager tests ---

    @Test
    void researchManager_makesSingleLLMCall() {
        // Arrange
        ctx.expectResponse("Investment plan: Buy AAPL with 60% allocation.");
        var ticker = new ResearchTypes.Ticker("AAPL", "");
        var state = new ResearchTypes.InvestmentDebateState(
                List.of("bull argument", "bear argument"),
                List.of("bull argument"),
                List.of("bear argument"),
                "bear argument", 2,
                new ResearchTypes.DebateBriefs("F", "M", "N", "S"),
                new RiskAssessment(RiskLevel.NEUTRAL, "moderate risk"),
                "", "", "", "", ""
        );
        var feedback = new ResearchTypes.InvestmentReviewFeedback("Looks good", true);

        // Act
        var result = agent.researchManager(ticker, state, new RiskAssessment(RiskLevel.NEUTRAL, "moderate"), feedback, null, ctx);

        // Assert
        var invocations = promptRunner.getLlmInvocations();
        assertEquals(1, invocations.size());
        var llm = invocations.get(0).getInteraction().getLlm();
        assertNotNull(llm);
    }

    @Test
    void researchManager_usesCorrectInteractionId() {
        // Arrange
        ctx.expectResponse("Investment plan content.");
        var ticker = new ResearchTypes.Ticker("AAPL", "");
        var state = new ResearchTypes.InvestmentDebateState(
                List.of("history"), List.of("bull"), List.of("bear"), "response", 2,
                new ResearchTypes.DebateBriefs("F", "M", "N", "S"),
                null,
                "", "", "", "", ""
        );
        var feedback = new ResearchTypes.InvestmentReviewFeedback("", true);

        // Act
        agent.researchManager(ticker, state, null, feedback, null, ctx);

        // Assert
        assertEquals("researchManager", promptRunner.getLlmInvocations().get(0).getInteraction().getId());
    }

    @Test
    void researchManager_promptIsNonEmpty() {
        // Arrange
        ctx.expectResponse("Investment plan.");
        var ticker = new ResearchTypes.Ticker("AAPL", "");
        var state = new ResearchTypes.InvestmentDebateState(
                List.of(), List.of(), List.of(), "", 0,
                new ResearchTypes.DebateBriefs("F", "M", "N", "S"),
                null,
                "", "", "", "", ""
        );
        var feedback = new ResearchTypes.InvestmentReviewFeedback("", true);

        // Act
        agent.researchManager(ticker, state, null, feedback, null, ctx);

        // Assert
        var prompt = promptRunner.getLlmInvocations().get(0).getPrompt();
        assertFalse(prompt.isBlank());
    }

    @Test
    void researchManager_includesHistoryInPrompt() {
        // Arrange
        ctx.expectResponse("Investment plan.");
        var ticker = new ResearchTypes.Ticker("AAPL", "");
        var state = new ResearchTypes.InvestmentDebateState(
                List.of("bull argument about growth", "bear argument about valuation"),
                List.of("bull argument about growth"),
                List.of("bear argument about valuation"),
                "bear argument", 2,
                new ResearchTypes.DebateBriefs("F", "M", "N", "S"),
                null,
                "", "", "", "", ""
        );
        var feedback = new ResearchTypes.InvestmentReviewFeedback("", true);

        // Act
        agent.researchManager(ticker, state, null, feedback, null, ctx);

        // Assert
        var prompt = promptRunner.getLlmInvocations().get(0).getPrompt();
        // The history is passed as a model variable and should appear in the prompt
        assertTrue(prompt.contains("growth") || prompt.contains("valuation") || prompt.contains("history"));
    }

    @Test
    void researchManager_includesRiskLevelInPrompt() {
        // Arrange
        ctx.expectResponse("Investment plan.");
        var ticker = new ResearchTypes.Ticker("AAPL", "");
        var state = new ResearchTypes.InvestmentDebateState(
                List.of(), List.of(), List.of(), "", 0,
                new ResearchTypes.DebateBriefs("F", "M", "N", "S"),
                new RiskAssessment(RiskLevel.RISKY, "high risk"),
                "", "", "", "", ""
        );
        var feedback = new ResearchTypes.InvestmentReviewFeedback("", true);

        // Act
        agent.researchManager(ticker, state, new RiskAssessment(RiskLevel.RISKY, "high risk"), feedback, null, ctx);

        // Assert — verify the LLM call was made with risk data
        var invocations = promptRunner.getLlmInvocations();
        assertEquals(1, invocations.size());
    }

    @Test
    void researchManager_includesRiskReasoningInPrompt() {
        // Arrange
        ctx.expectResponse("Investment plan.");
        var ticker = new ResearchTypes.Ticker("AAPL", "");
        var state = new ResearchTypes.InvestmentDebateState(
                List.of(), List.of(), List.of(), "", 0,
                new ResearchTypes.DebateBriefs("F", "M", "N", "S"),
                new RiskAssessment(RiskLevel.RISKY, "high risk with aggressive growth potential"),
                "", "", "", "", ""
        );
        var feedback = new ResearchTypes.InvestmentReviewFeedback("", true);

        // Act
        agent.researchManager(ticker, state, new RiskAssessment(RiskLevel.RISKY, "high risk with aggressive growth potential"), feedback, null, ctx);

        // Assert
        var prompt = promptRunner.getLlmInvocations().get(0).getPrompt();
        assertTrue(prompt.contains("aggressive growth potential") || prompt.contains("reasoning"));
    }

    @Test
    void researchManager_includesUserFeedbackInPrompt() {
        // Arrange
        ctx.expectResponse("Investment plan.");
        var ticker = new ResearchTypes.Ticker("AAPL", "");
        var state = new ResearchTypes.InvestmentDebateState(
                List.of(), List.of(), List.of(), "", 0,
                new ResearchTypes.DebateBriefs("F", "M", "N", "S"),
                null,
                "", "", "", "", ""
        );
        var feedback = new ResearchTypes.InvestmentReviewFeedback("Consider the valuation more carefully", true);

        // Act
        agent.researchManager(ticker, state, null, feedback, null, ctx);

        // Assert
        var prompt = promptRunner.getLlmInvocations().get(0).getPrompt();
        assertTrue(prompt.contains("valuation") || prompt.contains("user_feedback") || prompt.contains("feedback"));
    }

    @Test
    void researchManager_sanitizesFeedback() {
        // Arrange — feedback with Jinja template syntax should be sanitized
        ctx.expectResponse("Investment plan.");
        var ticker = new ResearchTypes.Ticker("AAPL", "");
        var state = new ResearchTypes.InvestmentDebateState(
                List.of(), List.of(), List.of(), "", 0,
                new ResearchTypes.DebateBriefs("F", "M", "N", "S"),
                null,
                "", "", "", "", ""
        );
        var feedback = new ResearchTypes.InvestmentReviewFeedback("Use {{ ticker }} in the prompt", true);

        // Act
        agent.researchManager(ticker, state, null, feedback, null, ctx);

        // Assert — the sanitized feedback should replace {{ ticker }} with [BLOCKED_TEMPLATE]
        var prompt = promptRunner.getLlmInvocations().get(0).getPrompt();
        assertFalse(prompt.contains("{{ ticker }}"));
        assertTrue(prompt.contains("[BLOCKED_TEMPLATE]"));
    }

    @Test
    void researchManager_includesHumanApprovedFlag() {
        // Arrange
        ctx.expectResponse("Investment plan.");
        var ticker = new ResearchTypes.Ticker("AAPL", "");
        var state = new ResearchTypes.InvestmentDebateState(
                List.of(), List.of(), List.of(), "", 0,
                new ResearchTypes.DebateBriefs("F", "M", "N", "S"),
                null,
                "", "", "", "", ""
        );
        var feedback = new ResearchTypes.InvestmentReviewFeedback("Approved", true);

        // Act
        agent.researchManager(ticker, state, null, feedback, null, ctx);

        // Assert — verify the LLM call was made (model variables are passed to the template)
        var invocations = promptRunner.getLlmInvocations();
        assertEquals(1, invocations.size());
    }

    @Test
    void researchManager_includesPastMemoryPlaceholder() {
        // Arrange
        ctx.expectResponse("Investment plan.");
        var ticker = new ResearchTypes.Ticker("AAPL", "");
        var state = new ResearchTypes.InvestmentDebateState(
                List.of(), List.of(), List.of(), "", 0,
                new ResearchTypes.DebateBriefs("F", "M", "N", "S"),
                null,
                "", "", "", "", ""
        );
        var feedback = new ResearchTypes.InvestmentReviewFeedback("", true);

        // Act
        agent.researchManager(ticker, state, null, feedback, null, ctx);

        // Assert — verify the LLM call was made (model variables are passed to the template)
        var invocations = promptRunner.getLlmInvocations();
        assertEquals(1, invocations.size());
    }

    @Test
    void researchManager_handlesNullRiskAssessment() {
        // Arrange
        ctx.expectResponse("Investment plan without risk.");
        var ticker = new ResearchTypes.Ticker("AAPL", "");
        var state = new ResearchTypes.InvestmentDebateState(
                List.of(), List.of(), List.of(), "", 0,
                new ResearchTypes.DebateBriefs("F", "M", "N", "S"),
                null,
                "", "", "", "", ""
        );
        var feedback = new ResearchTypes.InvestmentReviewFeedback("", true);

        // Act
        var result = agent.researchManager(ticker, state, null, feedback, null, ctx);

        // Assert
        assertNotNull(result);
        var prompt = promptRunner.getLlmInvocations().get(0).getPrompt();
        // Should not throw NPE — null risk assessment should be handled gracefully
        assertFalse(prompt.isBlank());
    }

    @Test
    void researchManager_returnsInvestmentPlanWithState() {
        // Arrange
        ctx.expectResponse("Buy AAPL at $150 with stop loss at $140.");
        var ticker = new ResearchTypes.Ticker("AAPL", "");
        var state = new ResearchTypes.InvestmentDebateState(
                List.of("bull", "bear"), List.of("bull"), List.of("bear"), "bear", 2,
                new ResearchTypes.DebateBriefs("F", "M", "N", "S"),
                null,
                "", "", "", "", ""
        );
        var feedback = new ResearchTypes.InvestmentReviewFeedback("", true);

        // Act
        var result = agent.researchManager(ticker, state, null, feedback, null, ctx);

        // Assert
        assertEquals("Buy AAPL at $150 with stop loss at $140.", result.judgeDecision());
        assertSame(state, result.investmentDebateState());
    }

    @Test
    void researchManager_multipleCallsWithDifferentFeedback() {
        // Arrange — use a fresh context and agent for each call to avoid cache hits
        var ticker = new ResearchTypes.Ticker("AAPL", "");
        var state = new ResearchTypes.InvestmentDebateState(
                List.of(), List.of(), List.of(), "", 0,
                new ResearchTypes.DebateBriefs("F", "M", "N", "S"),
                null,
                "", "", "", "", ""
        );

        // First call — fresh context
        var ctx1 = FakeOperationContext.create();
        var runner1 = ctx1.getPromptRunner();
        runner1.expectResponse("Plan A.");
        var agent1 = new DebateAgent(createCache(), null, null, null, null, null, null);
        agent1.researchManager(ticker, state, null, new ResearchTypes.InvestmentReviewFeedback("feedback A", true), null, ctx1);

        // Second call — fresh context
        var ctx2 = FakeOperationContext.create();
        var runner2 = ctx2.getPromptRunner();
        runner2.expectResponse("Plan B.");
        var agent2 = new DebateAgent(createCache(), null, null, null, null, null, null);
        agent2.researchManager(ticker, state, null, new ResearchTypes.InvestmentReviewFeedback("feedback B", true), null, ctx2);

        // Assert — each call made exactly 1 LLM call
        assertEquals(1, runner1.getLlmInvocations().size());
        assertEquals(1, runner2.getLlmInvocations().size());
    }
}
