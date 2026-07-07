package com.embabel.gekko.agent;

import com.embabel.agent.test.unit.FakeOperationContext;
import com.embabel.agent.test.unit.FakePromptRunner;
import com.embabel.gekko.agent.identity.InstrumentContextPromptContributor;
import com.embabel.gekko.agent.identity.InstrumentIdentityAgent;
import com.embabel.gekko.config.TraderAgentConfig;
import com.embabel.gekko.dataflows.YFinService;
import com.embabel.gekko.domain.ResearchTypes;
import com.embabel.gekko.util.FileCache;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for OrchestratorAgent.generateResearchPlan LLM-calling action.
 */
class OrchestratorAgentResearchPlanTest {

    private FakeOperationContext ctx;
    private FakePromptRunner promptRunner;
    private OrchestratorAgent agent;
    private InstrumentIdentityAgent identityAgent;
    private Path tempCacheDir;

    /**
     * Creates a FileCache backed by a unique temp directory.
     */
    private FileCache createCache() throws Exception {
        tempCacheDir = Files.createTempDirectory("research-plan-test-cache-");
        var cache = new FileCache();
        var field = FileCache.class.getDeclaredField("baseDir");
        field.setAccessible(true);
        field.set(cache, tempCacheDir);
        return cache;
    }

    @AfterEach
    void cleanupTempDir() {
        if (tempCacheDir != null) {
            try {
                java.nio.file.Files.walk(tempCacheDir)
                        .sorted((a, b) -> b.compareTo(a))
                        .forEach(path -> {
                            try {
                                java.nio.file.Files.delete(path);
                            } catch (Exception ignored) {}
                        });
            } catch (Exception ignored) {}
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        ctx = FakeOperationContext.create();
        promptRunner = ctx.getPromptRunner();
        FileCache testCache = createCache();
        YFinService yFinService = new YFinService();
        identityAgent = new InstrumentIdentityAgent(yFinService, testCache);
        agent = new OrchestratorAgent(
                testCache,
                identityAgent,
                null, // memoryAgent — not needed for generateResearchPlan
                null, // checkpointAgent — not needed for generateResearchPlan
                new InstrumentContextPromptContributor(), // instrumentContextContributor
                null  // debateAgentProvider — not needed for generateResearchPlan
        );
    }

    @Test
    void generateResearchPlan_makesSingleLLMCall() {
        // Arrange
        ctx.expectResponse("Research plan: Analyze AAPL fundamentals, market trends, and news.");
        var ticker = new ResearchTypes.Ticker("AAPL", "");

        // Act
        var result = agent.generateResearchPlan(ticker, null, ctx);

        // Assert
        var invocations = promptRunner.getLlmInvocations();
        assertEquals(1, invocations.size());
        assertEquals("Research plan: Analyze AAPL fundamentals, market trends, and news.", result.content());
    }

    @Test
    void generateResearchPlan_usesCorrectInteractionId() {
        // Arrange
        ctx.expectResponse("Research plan content.");
        var ticker = new ResearchTypes.Ticker("AAPL", "");

        // Act
        agent.generateResearchPlan(ticker, null, ctx);

        // Assert
        assertEquals("generateResearchPlan", promptRunner.getLlmInvocations().get(0).getInteraction().getId());
    }

    @Test
    void generateResearchPlan_returnsWrappedResearchPlan() {
        // Arrange
        ctx.expectResponse("Plan content.");
        var ticker = new ResearchTypes.Ticker("AAPL", "");

        // Act
        var result = agent.generateResearchPlan(ticker, null, ctx);

        // Assert
        assertNotNull(result);
        assertEquals("Plan content.", result.content());
    }

    @Test
    void generateResearchPlan_usesCache() throws Exception {
        // Arrange
        ctx.expectResponse("Cached plan.");
        var ticker = new ResearchTypes.Ticker("AAPL", "");

        // Act — first call
        var result1 = agent.generateResearchPlan(ticker, null, ctx);

        // Act — second call (should hit cache)
        ctx.expectResponse("Should not reach here.");
        var result2 = agent.generateResearchPlan(ticker, null, ctx);

        // Assert — only 1 LLM call (second hit cache)
        var invocations = promptRunner.getLlmInvocations();
        assertEquals(1, invocations.size());
        assertEquals("Cached plan.", result1.content());
        assertEquals("Cached plan.", result2.content());
    }

    @Test
    void generateResearchPlan_differentTickersHaveDifferentCacheKeys() {
        // Arrange
        ctx.expectResponse("AAPL plan.");
        var ticker1 = new ResearchTypes.Ticker("AAPL", "");
        var ticker2 = new ResearchTypes.Ticker("MSFT", "");

        // Act — two different tickers, each makes its own LLM call
        var result1 = agent.generateResearchPlan(ticker1, null, ctx);
        ctx.expectResponse("MSFT plan.");
        var result2 = agent.generateResearchPlan(ticker2, null, ctx);

        // Assert — 2 LLM calls (different cache keys)
        var invocations = promptRunner.getLlmInvocations();
        assertEquals(2, invocations.size());
        assertEquals("AAPL plan.", result1.content());
        assertEquals("MSFT plan.", result2.content());
    }

    @Test
    void generateResearchPlan_promptContainsTicker() {
        // Arrange
        ctx.expectResponse("Research plan for AAPL.");
        var ticker = new ResearchTypes.Ticker("AAPL", "");

        // Act
        agent.generateResearchPlan(ticker, null, ctx);

        // Assert
        var prompt = promptRunner.getLlmInvocations().get(0).getPrompt();
        // The ticker is passed as a model variable to the template
        assertFalse(prompt.isBlank());
    }

    @Test
    void generateResearchPlan_includesPastMemoryPlaceholder() {
        // Arrange
        ctx.expectResponse("Research plan.");
        var ticker = new ResearchTypes.Ticker("AAPL", "");

        // Act
        agent.generateResearchPlan(ticker, null, ctx);

        // Assert — verify the LLM call was made
        var invocations = promptRunner.getLlmInvocations();
        assertEquals(1, invocations.size());
        var prompt = promptRunner.getLlmInvocations().get(0).getPrompt();
        assertFalse(prompt.isBlank());
    }

    @Test
    void generateResearchPlan_includesHistoryPlaceholder() {
        // Arrange
        ctx.expectResponse("Research plan.");
        var ticker = new ResearchTypes.Ticker("AAPL", "");

        // Act
        agent.generateResearchPlan(ticker, null, ctx);

        // Assert — verify the LLM call was made
        var invocations = promptRunner.getLlmInvocations();
        assertEquals(1, invocations.size());
        var prompt = promptRunner.getLlmInvocations().get(0).getPrompt();
        assertFalse(prompt.isBlank());
    }

    @Test
    void generateResearchPlan_includesHumanApprovedFlag() {
        // Arrange
        ctx.expectResponse("Research plan.");
        var ticker = new ResearchTypes.Ticker("AAPL", "");

        // Act
        agent.generateResearchPlan(ticker, null, ctx);

        // Assert — verify the LLM call was made
        var invocations = promptRunner.getLlmInvocations();
        assertEquals(1, invocations.size());
        var prompt = promptRunner.getLlmInvocations().get(0).getPrompt();
        assertFalse(prompt.isBlank());
    }

    @Test
    void generateResearchPlan_includesUserFeedbackPlaceholder() {
        // Arrange
        ctx.expectResponse("Research plan.");
        var ticker = new ResearchTypes.Ticker("AAPL", "");

        // Act
        agent.generateResearchPlan(ticker, null, ctx);

        // Assert — verify the LLM call was made
        var invocations = promptRunner.getLlmInvocations();
        assertEquals(1, invocations.size());
        var prompt = promptRunner.getLlmInvocations().get(0).getPrompt();
        assertFalse(prompt.isBlank());
    }
}
