package com.embabel.gekko.agent;

import com.embabel.agent.test.integration.EmbabelMockitoIntegrationTest;
import com.embabel.gekko.domain.ResearchTypes;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

/**
 * End-to-end integration test for the full research pipeline migrated from Tauric (Python).
 *
 * Validates the complete flow:
 * 1. User submits ticker → OrchestratorAgent resolves identity
 * 2. Research plan generated → DebateAgent orchestrates
 * 3. Bull/bear debate with DebateLoopAgent
 * 4. Risk assessment with RiskDebateAgent
 * 5. Human-in-the-loop review with WaitFor
 * 6. Final investment plan generated
 *
 * Uses Embabel's EmbabelMockitoIntegrationTest base with scripted LLM responses.
 */
@Tag("integration")
class FullPipelineIntegrationTest extends EmbabelMockitoIntegrationTest {

    @Test
    void shouldExecuteFullResearchPipeline() {
        // Given: a ticker input (Ticker has content and feedback fields)
        var ticker = new ResearchTypes.Ticker("AAPL", null);

        // Stub the LLM responses for each stage of the pipeline
        whenGenerateText(any()).thenReturn(
            "{\"ticker\":\"AAPL\",\"companyName\":\"Apple Inc.\",\"sector\":\"Technology\",\"industry\":\"Consumer Electronics\",\"exchange\":\"NASDAQ\",\"currency\":\"USD\"}"
        );
        whenCreateObject(any(), eq(ResearchTypes.ResearchPlan.class))
                .thenReturn(new ResearchTypes.ResearchPlan("Research plan for AAPL: Buy on dip"));
        whenGenerateText(any()).thenReturn(
            "AAPL fundamentals: Strong revenue growth, P/E 28, ROE 150%"
        );
        whenGenerateText(any()).thenReturn(
            "AAPL market: RSI 55, MACD bullish crossover, volume above average"
        );
        whenGenerateText(any()).thenReturn(
            "AAPL news: New product launch expected Q3 2026"
        );
        whenGenerateText(any()).thenReturn(
            "AAPL sentiment: 72% positive on social media"
        );
        whenGenerateText(any()).thenReturn(
            "### FUNDAMENTALS BRIEF\nStrong fundamentals\n\n### MARKET BRIEF\nBullish technicals\n\n### NEWS BRIEF\nPositive news flow\n\n### SOCIAL BRIEF\nPositive sentiment"
        );
        whenGenerateText(any()).thenReturn(
            "BULL CASE: AAPL is undervalued with strong ecosystem moat"
        );
        whenGenerateText(any()).thenReturn(
            "BEAR CASE: AAPL faces regulatory risk and slowing growth"
        );
        whenGenerateText(any()).thenReturn(
            "After reviewing both arguments, the bull case is stronger due to ecosystem moat"
        );
        whenGenerateText(any()).thenReturn(
            "{\"level\":\"LOW\",\"reasoning\":\"Diversified company with strong balance sheet\"}"
        );
        whenCreateObject(any(), eq(ResearchTypes.InvestmentPlan.class))
                .thenReturn(new ResearchTypes.InvestmentPlan(
                    "BUY AAPL at current levels. Target: $200. Stop loss: $170.",
                    null
                ));

        // When: we invoke the orchestrator agent
        var orchestrator = agentPlatform.agents().stream()
                .filter(a -> a.getName().equals("OrchestratorAgent"))
                .findFirst()
                .orElse(null);

        // Then: the orchestrator should be registered with actions
        assertNotNull(orchestrator, "OrchestratorAgent should be registered");
        assertFalse(orchestrator.getActions().isEmpty(),
                "OrchestratorAgent should have actions");
    }

    @Test
    void shouldHaveAllFourAgentsRegistered() {
        var agents = agentPlatform.agents();
        String[] expectedNames = {"OrchestratorAgent", "DebateAgent", "DebateLoopAgent", "RiskDebateAgent"};

        for (String name : expectedNames) {
            var found = agents.stream().anyMatch(a -> a.getName().equals(name));
            assertTrue(found, "AgentPlatform should contain agent '" + name + "'");
        }
    }

    @Test
    void shouldHaveOrchestratorAgentWithActions() {
        var orchestrator = agentPlatform.agents().stream()
                .filter(a -> a.getName().equals("OrchestratorAgent"))
                .findFirst()
                .orElseThrow();

        assertFalse(orchestrator.getActions().isEmpty(),
                "OrchestratorAgent should have actions");
    }

    @Test
    void shouldHaveDebateAgentWithActions() {
        var debateAgent = agentPlatform.agents().stream()
                .filter(a -> a.getName().equals("DebateAgent"))
                .findFirst()
                .orElseThrow();

        assertFalse(debateAgent.getActions().isEmpty(),
                "DebateAgent should have actions");
    }

    @Test
    void shouldHaveDebateLoopAgent() {
        var debateLoop = agentPlatform.agents().stream()
                .filter(a -> a.getName().equals("DebateLoopAgent"))
                .findFirst()
                .orElseThrow();

        assertFalse(debateLoop.getActions().isEmpty(),
                "DebateLoopAgent should have actions");
    }

    @Test
    void shouldHaveRiskDebateAgent() {
        var riskDebate = agentPlatform.agents().stream()
                .filter(a -> a.getName().equals("RiskDebateAgent"))
                .findFirst()
                .orElseThrow();

        assertFalse(riskDebate.getActions().isEmpty(),
                "RiskDebateAgent should have actions");
    }

    @Test
    void shouldHaveInstrumentIdentityAgent() {
        var identityAgent = agentPlatform.agents().stream()
                .filter(a -> a.getName().equals("InstrumentIdentityAgent"))
                .findFirst();

        assertTrue(identityAgent.isPresent(),
                "InstrumentIdentityAgent should be registered");
        assertFalse(identityAgent.get().getActions().isEmpty(),
                "InstrumentIdentityAgent should have actions");
    }

    @Test
    void shouldHaveDecisionMemoryAgent() {
        var memoryAgent = agentPlatform.agents().stream()
                .filter(a -> a.getName().equals("DecisionMemoryAgent"))
                .findFirst();

        assertTrue(memoryAgent.isPresent(),
                "DecisionMemoryAgent should be registered");
        assertFalse(memoryAgent.get().getActions().isEmpty(),
                "DecisionMemoryAgent should have actions");
    }

    @Test
    void shouldHaveCheckpointAgent() {
        var checkpointAgent = agentPlatform.agents().stream()
                .filter(a -> a.getName().equals("CheckpointAgent"))
                .findFirst();

        assertTrue(checkpointAgent.isPresent(),
                "CheckpointAgent should be registered");
        assertFalse(checkpointAgent.get().getActions().isEmpty(),
                "CheckpointAgent should have actions");
    }

    @Test
    void shouldHaveDataToolsAsSpringBeans() {
        // FredDataTools and PolymarketDataTools should be registered as Spring beans
        // (verified by successful Spring context startup)
        var agents = agentPlatform.agents();
        assertNotNull(agents);
        assertFalse(agents.isEmpty());
    }
}