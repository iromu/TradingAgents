package com.embabel.gekko.agent;

import com.embabel.agent.test.integration.EmbabelMockitoIntegrationTest;
import com.embabel.gekko.config.TraderAgentConfig;
import com.embabel.gekko.domain.ResearchTypes;
import com.embabel.gekko.agent.researchers.BearResearcher;
import com.embabel.gekko.agent.researchers.BullResearcher;
import com.embabel.gekko.util.FileCache;
import com.embabel.gekko.util.LlmBudgetTracker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for DebateLoopAgent.debate() with LLM calls.
 * Verifies the full debate loop with BullResearcher and BearResearcher.
 */
@Tag("integration")
class DebateLoopAgentIntegrationTest extends EmbabelMockitoIntegrationTest {

    private Path tempCacheDir;
    private FileCache cache;

    @Autowired
    private LlmBudgetTracker llmBudgetTracker;

    @Autowired
    private TraderAgentConfig config;

    @AfterEach
    void cleanupTempDir() {
        if (tempCacheDir != null) {
            try {
                java.nio.file.Files.walk(tempCacheDir)
                        .sorted((a, b) -> b.compareTo(a))
                        .forEach(path -> {
                            try { java.nio.file.Files.delete(path); } catch (Exception ignored) {}
                        });
            } catch (Exception ignored) {}
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        tempCacheDir = Files.createTempDirectory("debate-loop-agent-integration-test-cache-");
        cache = new FileCache();
        var field = FileCache.class.getDeclaredField("baseDir");
        field.setAccessible(true);
        field.set(cache, tempCacheDir);
    }

    @Test
    void debate_returnsInvestmentDebateState() {
        var bullResearcher = new BullResearcher();
        var bearResearcher = new BearResearcher();

        // Create a config with 2 iterations, 0.0 threshold (never converges)
        var testConfig = new TraderAgentConfig(
                config.tickerLlm(), config.writerLlm(), config.maxConcurrency(),
                config.researcher(), config.outliner(), config.writer(),
                config.outputDirectory(), 1.0, 2,  // 0.0 threshold = never converges, 2 max iterations
                config.provider(), config.bestModel(), config.cheapestModel(),
                config.anthropic(), config.google(), config.openai()
        );

        var agent = new DebateLoopAgent(bullResearcher, bearResearcher, cache, testConfig, llmBudgetTracker);

        var ticker = new ResearchTypes.Ticker("AAPL", "");
        var briefs = new ResearchTypes.DebateBriefs("F", "M", "N", "S");

        var fake = FakeActionContext.create();
        var context = fake.getActionContext();

        // Stub LLM responses for bull and bear argue() calls
        // 2 iterations = 4 responses (bull, bear, bull, bear)
        for (int i = 0; i < 10; i++) {
            fake.getDelegate().expectResponse("Debate response " + i);
        }

        var result = agent.debate(ticker, briefs, context);

        assertNotNull(result);
        assertNotNull(result.history());
        assertFalse(result.history().isEmpty());
        assertNotNull(result.bullHistory());
        assertNotNull(result.bearHistory());
    }

    @Test
    void debate_includesBullAndBearResponses() {
        var bullResearcher = new BullResearcher();
        var bearResearcher = new BearResearcher();

        var testConfig = new TraderAgentConfig(
                config.tickerLlm(), config.writerLlm(), config.maxConcurrency(),
                config.researcher(), config.outliner(), config.writer(),
                config.outputDirectory(), 1.0, 2,  // 2 iterations, never converges
                config.provider(), config.bestModel(), config.cheapestModel(),
                config.anthropic(), config.google(), config.openai()
        );

        var agent = new DebateLoopAgent(bullResearcher, bearResearcher, cache, testConfig, llmBudgetTracker);

        var ticker = new ResearchTypes.Ticker("AAPL", "");
        var briefs = new ResearchTypes.DebateBriefs("F", "M", "N", "S");

        var fake = FakeActionContext.create();
        var context = fake.getActionContext();

        // 2 iterations = 4 responses (bull, bear, bull, bear)
        for (int i = 0; i < 10; i++) {
            fake.getDelegate().expectResponse("Response " + i);
        }

        var result = agent.debate(ticker, briefs, context);

        assertNotNull(result);
        // With 2 max iterations, should have 4 responses (2 bull + 2 bear)
        assertEquals(4, result.history().size());
        assertEquals(2, result.bullHistory().size());
        assertEquals(2, result.bearHistory().size());
    }

    @Test
    void debate_stopsAtMaxIterations() {
        var bullResearcher = new BullResearcher();
        var bearResearcher = new BearResearcher();

        var testConfig = new TraderAgentConfig(
                config.tickerLlm(), config.writerLlm(), config.maxConcurrency(),
                config.researcher(), config.outliner(), config.writer(),
                config.outputDirectory(), 1.0, 3,  // 3 max iterations, 0.0 threshold = never converges
                config.provider(), config.bestModel(), config.cheapestModel(),
                config.anthropic(), config.google(), config.openai()
        );

        var agent = new DebateLoopAgent(bullResearcher, bearResearcher, cache, testConfig, llmBudgetTracker);

        var ticker = new ResearchTypes.Ticker("AAPL", "");
        var briefs = new ResearchTypes.DebateBriefs("F", "M", "N", "S");

        var fake = FakeActionContext.create();
        var context = fake.getActionContext();

        // 3 iterations = 6 responses (bull, bear, bull, bear, bull, bear)
        for (int i = 0; i < 10; i++) {
            fake.getDelegate().expectResponse("Response " + i);
        }

        var result = agent.debate(ticker, briefs, context);

        // Should stop at 3 iterations = 6 responses
        assertEquals(6, result.history().size());
    }

    @Test
    void debate_withCacheHits() throws Exception {
        var bullResearcher = new BullResearcher();
        var bearResearcher = new BearResearcher();

        var testConfig = new TraderAgentConfig(
                config.tickerLlm(), config.writerLlm(), config.maxConcurrency(),
                config.researcher(), config.outliner(), config.writer(),
                config.outputDirectory(), 1.0, 1,  // 1 iteration only
                config.provider(), config.bestModel(), config.cheapestModel(),
                config.anthropic(), config.google(), config.openai()
        );

        var agent = new DebateLoopAgent(bullResearcher, bearResearcher, cache, testConfig, llmBudgetTracker);

        var ticker = new ResearchTypes.Ticker("AAPL", "");
        var briefs = new ResearchTypes.DebateBriefs("F", "M", "N", "S");

        var fake = FakeActionContext.create();
        var context = fake.getActionContext();

        // Stub only 2 responses (1 bull + 1 bear)
        fake.getDelegate().expectResponse("Cached bull response");
        fake.getDelegate().expectResponse("Cached bear response");

        var result = agent.debate(ticker, briefs, context);

        assertNotNull(result);
        assertNotNull(result.history());
    }

    @Test
    void debate_convergesEarlyOnSimilarResponses() {
        var bullResearcher = new BullResearcher();
        var bearResearcher = new BearResearcher();

        // Set threshold to 0.0 so ANY similarity triggers convergence
        var testConfig = new TraderAgentConfig(
                config.tickerLlm(), config.writerLlm(), config.maxConcurrency(),
                config.researcher(), config.outliner(), config.writer(),
                config.outputDirectory(), 0.0, 5,  // 0.0 threshold = converges after 2nd iteration, 5 max iterations
                config.provider(), config.bestModel(), config.cheapestModel(),
                config.anthropic(), config.google(), config.openai()
        );

        var agent = new DebateLoopAgent(bullResearcher, bearResearcher, cache, testConfig, llmBudgetTracker);

        var ticker = new ResearchTypes.Ticker("AAPL", "");
        var briefs = new ResearchTypes.DebateBriefs("F", "M", "N", "S");

        var fake = FakeActionContext.create();
        var context = fake.getActionContext();

        // Use identical responses for bull — with 0.0 threshold, convergence triggers after 2nd iteration
        // (need 2 bull responses to compare). Runs 2 iterations = 4 responses total.
        for (int i = 0; i < 10; i++) {
            fake.getDelegate().expectResponse("Identical bull argument");
            fake.getDelegate().expectResponse("Identical bear argument");
        }

        var result = agent.debate(ticker, briefs, context);

        // Converges after 2 iterations (4 responses) because after the 2nd bull response,
        // similarity = 1.0 >= 0.0 threshold. Does NOT run all 5 iterations (10 responses).
        assertEquals(4, result.history().size());
        assertEquals(2, result.bullHistory().size());
        assertEquals(2, result.bearHistory().size());
    }

    @Test
    void debate_actionExistsOnAgent() {
        // Verify the agent is registered
        var debateLoopAgent = agentPlatform.agents().stream()
                .filter(a -> a.getName().equals("DebateLoopAgent"))
                .findFirst()
                .orElseThrow();

        assertNotNull(debateLoopAgent);
        assertFalse(debateLoopAgent.getActions().isEmpty());
    }
}