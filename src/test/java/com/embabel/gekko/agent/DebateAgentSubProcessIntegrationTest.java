package com.embabel.gekko.agent;

import com.embabel.agent.api.common.ActionContext;
import com.embabel.agent.test.integration.EmbabelMockitoIntegrationTest;
import com.embabel.gekko.domain.ResearchTypes;
import com.embabel.gekko.util.FileCache;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for DebateAgent sub-process actions:
 * runDebate, runTrader, runRiskDebate, runPortfolioManager.
 */
@Tag("integration")
class DebateAgentSubProcessIntegrationTest extends EmbabelMockitoIntegrationTest {

    private Path tempCacheDir;
    private FileCache cache;
    private DebateAgent agent;
    private FakeActionContext fake;
    private ActionContext context;

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
        tempCacheDir = Files.createTempDirectory("debate-agent-subprocess-test-cache-");
        cache = new FileCache();
        var field = FileCache.class.getDeclaredField("baseDir");
        field.setAccessible(true);
        field.set(cache, tempCacheDir);

        agent = new DebateAgent(
                cache,
                null, // templateRenderer
                null, // memoryAgent
                null, // debateLoopAgentProvider
                null, // riskDebateAgentProvider
                null, // traderProvider
                null, // portfolioManagerProvider
                null  // llmBudgetTracker
        );

        fake = FakeActionContext.create();
        context = fake.getActionContext();
    }

    @Test
    void runDebate_delegatesToDebateLoopAgent() {
        // Stub the debate loop responses
        var debateLoopAgent = agentPlatform.agents().stream()
                .filter(a -> a.getName().equals("DebateLoopAgent"))
                .findFirst()
                .orElseThrow();

        // Stub the LLM responses for the debate loop (bull/bear pairs)
        fake.getDelegate().expectResponse("Bull argument 1");
        fake.getDelegate().expectResponse("Bear argument 1");
        fake.getDelegate().expectResponse("Bull argument 2");
        fake.getDelegate().expectResponse("Bear argument 2");

        var ticker = new ResearchTypes.Ticker("AAPL", "");
        var briefs = new ResearchTypes.DebateBriefs("F", "M", "N", "S");

        var result = agent.runDebate(ticker, briefs, context);

        // Verify the result is a valid InvestmentDebateState with history
        assertNotNull(result);
        assertNotNull(result.history());
        assertFalse(result.history().isEmpty());
        // Should have at least 4 entries (2 bull + 2 bear)
        assertTrue(result.history().size() >= 4);
    }

    @Test
    void runDebate_returnsInvestmentDebateState() {
        var ticker = new ResearchTypes.Ticker("AAPL", "");
        var briefs = new ResearchTypes.DebateBriefs("F", "M", "N", "S");

        // Stub the debate loop responses
        fake.getDelegate().expectResponse("Bull argument");
        fake.getDelegate().expectResponse("Bear argument");

        var result = agent.runDebate(ticker, briefs, context);

        // Verify the return type is InvestmentDebateState
        assertInstanceOf(ResearchTypes.InvestmentDebateState.class, result);
        // Verify the state has valid fields
        assertFalse(result.history().isEmpty());
        assertFalse(result.bullHistory().isEmpty());
        assertFalse(result.bearHistory().isEmpty());
    }

    @Test
    void runTrader_callsTraderDirectly() {
        var ticker = new ResearchTypes.Ticker("AAPL", "");
        var researchPlan = "AAPL is a strong buy based on fundamentals.";

        // Stub the LLM response for the Trader
        fake.getDelegate().expectResponse("Buy 50% of AAPL at $150.");
        fake.getDelegate().expectResponse("Buy 50% of AAPL at $150.");

        var trader = new com.embabel.gekko.agent.Trader();
        var result = trader.traderProposal(ticker, researchPlan, context);

        assertNotNull(result);
        assertFalse(result.isBlank());
    }

    @Test
    void runRiskDebate_delegatesToRiskDebateAgent() {
        var ticker = new ResearchTypes.Ticker("AAPL", "");
        var briefs = new ResearchTypes.DebateBriefs("F", "M", "N", "S");
        var debateState = new ResearchTypes.InvestmentDebateState(
                List.of("bull argument", "bear argument"),
                List.of("bull argument"),
                List.of("bear argument"),
                "bear argument", 2,
                briefs
        );
        var traderProposal = "Buy 50% at $150.";

        // Stub the risk debate responses (9 debator + 1 judge)
        for (int i = 0; i < 9; i++) {
            fake.getDelegate().expectResponse("Debate response " + i);
        }
        fake.getDelegate().expectResponse(new RiskAssessmentOutput(RiskLevel.NEUTRAL, "Moderate risk profile"));

        var result = agent.runRiskDebate(ticker, briefs, debateState, traderProposal, context);

        // Verify the result is a valid RiskAssessment
        assertNotNull(result);
        assertInstanceOf(RiskAssessment.class, result);
        assertEquals(RiskLevel.NEUTRAL, result.level());
        assertFalse(result.reasoning().isBlank());
    }

    @Test
    void runPortfolioManager_delegatesToPortfolioManager() {
        var ticker = new ResearchTypes.Ticker("AAPL", "");
        var debateState = new ResearchTypes.InvestmentDebateState(
                List.of("bull argument", "bear argument"),
                List.of("bull argument"),
                List.of("bear argument"),
                "bear argument", 2,
                new ResearchTypes.DebateBriefs("F", "M", "N", "S")
        );
        var researchPlan = "AAPL is a strong buy.";
        var traderProposal = "Buy 50% at $150.";
        var riskAssessment = new RiskAssessment(RiskLevel.NEUTRAL, "Moderate risk");

        // Stub the LLM responses for PortfolioManager (structured attempt + fallback)
        fake.getDelegate().expectResponse("BUY 50% of AAPL.");
        fake.getDelegate().expectResponse("BUY 50% of AAPL.");

        var portfolioManager = new com.embabel.gekko.agent.managers.PortfolioManager();
        var result = portfolioManager.portfolioDecision(
                ticker, debateState, researchPlan, traderProposal, riskAssessment, context
        );

        assertNotNull(result);
        assertFalse(result.isBlank());
    }

    @Test
    void runPortfolioManager_includesAllInputsInPrompt() {
        var ticker = new ResearchTypes.Ticker("NVDA", "");
        var debateState = new ResearchTypes.InvestmentDebateState(
                List.of("bull: AI growth", "bear: valuation concern"),
                List.of("bull: AI growth"),
                List.of("bear: valuation concern"),
                "bear: valuation concern", 2,
                new ResearchTypes.DebateBriefs("F", "M", "N", "S")
        );
        var researchPlan = "NVDA: Strong AI growth, buy at $500.";
        var traderProposal = "Buy 30% at $500, stop-loss $450.";
        var riskAssessment = new RiskAssessment(RiskLevel.RISKY, "High risk high reward");

        fake.getDelegate().expectResponse("BUY NVDA.");
        fake.getDelegate().expectResponse("BUY NVDA.");

        var portfolioManager = new com.embabel.gekko.agent.managers.PortfolioManager();
        portfolioManager.portfolioDecision(
                ticker, debateState, researchPlan, traderProposal, riskAssessment, context
        );

        var prompt = fake.getDelegate().getPromptRunner().getLlmInvocations().get(0).getPrompt();
        assertTrue(prompt.contains("NVDA"));
        assertTrue(prompt.contains("AI growth"));
        assertTrue(prompt.contains("valuation concern"));
        assertTrue(prompt.contains("Buy 30% at $500"));
        assertTrue(prompt.contains("RISKY"));
    }
}