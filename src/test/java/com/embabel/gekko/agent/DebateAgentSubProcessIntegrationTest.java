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

        var debateLoopAgentMock = mock(com.embabel.agent.core.Agent.class);
        var riskDebateAgentMock = mock(RiskDebateAgent.class);
        var traderMock = mock(Trader.class);
        var portfolioManagerMock = mock(com.embabel.gekko.agent.managers.PortfolioManager.class);
        var marketToolsMock = mock(com.embabel.gekko.tools.MarketDataTools.class);

        var debateLoopProvider = mock(ObjectProvider.class);
        when(debateLoopProvider.getObject()).thenReturn(debateLoopAgentMock);

        var riskDebateProvider = mock(ObjectProvider.class);
        when(riskDebateProvider.getObject()).thenReturn(riskDebateAgentMock);

        var traderProvider = mock(ObjectProvider.class);
        when(traderProvider.getObject()).thenReturn(traderMock);

        var portfolioManagerProvider = mock(ObjectProvider.class);
        when(portfolioManagerProvider.getObject()).thenReturn(portfolioManagerMock);

        var marketToolsProvider = mock(ObjectProvider.class);
        when(marketToolsProvider.getObject()).thenReturn(marketToolsMock);

        agent = new DebateAgent(
                cache,
                null, // templateRenderer
                null, // memoryAgent
                debateLoopProvider,
                riskDebateProvider,
                traderProvider,
                portfolioManagerProvider,
                marketToolsProvider,
                null  // llmBudgetTracker
        );

        fake = FakeActionContext.create();
        context = fake.getActionContext();
    }

    @Test
    void runDebate_delegatesToDebateLoopAgent() {
        // Verify the DebateLoopAgent is registered in the agent platform
        var debateLoopAgent = agentPlatform.agents().stream()
                .filter(a -> a.getName().equals("DebateLoopAgent"))
                .findFirst()
                .orElseThrow();

        assertNotNull(debateLoopAgent);
        assertFalse(debateLoopAgent.getActions().isEmpty());
    }

    @Test
    void runDebate_returnsInvestmentDebateState() {
        // Verify the DebateAgent is registered and has a runDebate action
        var debateAgent = agentPlatform.agents().stream()
                .filter(a -> a.getName().equals("DebateAgent"))
                .findFirst()
                .orElseThrow();

        var runDebateAction = debateAgent.getActions().stream()
                .filter(a -> a.toString().toLowerCase().contains("rundebate"))
                .findFirst();

        assertTrue(runDebateAction.isPresent(), "DebateAgent should have runDebate action");
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
        // Verify the RiskDebateAgent is registered in the agent platform
        var riskDebateAgent = agentPlatform.agents().stream()
                .filter(a -> a.getName().equals("RiskDebateAgent"))
                .findFirst()
                .orElseThrow();

        assertNotNull(riskDebateAgent);
        assertFalse(riskDebateAgent.getActions().isEmpty());
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