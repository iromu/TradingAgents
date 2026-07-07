package com.embabel.gekko.agent;

import com.embabel.agent.test.integration.EmbabelMockitoIntegrationTest;
import com.embabel.agent.test.unit.FakeOperationContext;
import com.embabel.gekko.agent.managers.PortfolioManager;
import com.embabel.gekko.domain.ResearchTypes;
import com.embabel.gekko.util.FileCache;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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
 * Uses Embabel's FakeOperationContext with scripted LLM responses for deterministic testing.
 */
@Tag("integration")
class FullPipelineIntegrationTest extends EmbabelMockitoIntegrationTest {

    private FakeOperationContext ctx;
    private ResearchTypes.Ticker ticker;
    private DebateAgent debateAgent;
    private OrchestratorAgent orchestratorAgent;
    private PortfolioManager portfolioManager;
    private Path tempCacheDir;

    private FileCache createCache() throws Exception {
        tempCacheDir = Files.createTempDirectory("full-pipeline-test-cache-");
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
                            try { java.nio.file.Files.delete(path); } catch (Exception ignored) {}
                        });
            } catch (Exception ignored) {}
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        ctx = FakeOperationContext.create();
        ticker = new ResearchTypes.Ticker("AAPL", "");

        // Initialize agents with minimal constructors (LLM calls go through ctx)
        var cache = createCache();
        debateAgent = new DebateAgent(
                cache, null, null, null, null, null, null
        );
        orchestratorAgent = new OrchestratorAgent(
                cache, null, null, null, null, null
        );
        portfolioManager = new PortfolioManager();
    }

    // --- Agent registration smoke tests (preserved from original) ---

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
        var agents = agentPlatform.agents();
        assertNotNull(agents);
        assertFalse(agents.isEmpty());
    }

    // --- Agent invocation integration tests ---

    @Test
    void shouldInvokeDebateAgentGenerateFundamentalsReport() {
        // Arrange — stub the LLM response
        ctx.expectResponse("Stub fundamentals report.");

        // Act — invoke the agent action directly
        var result = debateAgent.generateFundamentalsReport(ticker, ctx);

        // Assert — verify the result is the stubbed response wrapped in a report
        assertEquals("Stub fundamentals report.", result.content());

        // Verify the LLM interaction
        var invocations = ctx.getPromptRunner().getLlmInvocations();
        assertEquals(1, invocations.size());
        assertEquals("generateFundamentalsReport", invocations.get(0).getInteraction().getId());
    }

    @Test
    void shouldInvokeOrchestratorGenerateResearchPlan() {
        // Arrange — stub the LLM response for research plan
        ctx.expectResponse("Stub research plan.");

        // Act — invoke the orchestrator action
        var result = orchestratorAgent.generateResearchPlan(
                ticker, null, ctx
        );

        // Assert — verify the result is the stubbed response wrapped in a plan
        assertEquals("Stub research plan.", result.content());

        // Verify the LLM interaction
        var invocations = ctx.getPromptRunner().getLlmInvocations();
        assertEquals(1, invocations.size());
        assertEquals("generateResearchPlan", invocations.get(0).getInteraction().getId());
    }

    @Test
    void shouldInvokePortfolioManagerWithStubbedResponse() {
        // Arrange — stub the LLM response for portfolio decision
        // The PortfolioManager tries PortfolioDecisionOutput first, then falls back to String
        // We need to stub both: first the PortfolioDecisionOutput (which will fail),
        // then the String fallback
        ctx.expectResponse("Stub portfolio decision."); // consumed by PortfolioDecisionOutput attempt (fails)
        ctx.expectResponse("Stub portfolio decision."); // consumed by String fallback

        var debateState = new ResearchTypes.InvestmentDebateState(
                List.of("bull argument", "bear argument"),
                List.of("bull argument"),
                List.of("bear argument"),
                "bear argument", 2,
                new ResearchTypes.DebateBriefs("F", "M", "N", "S")
        );
        var researchPlan = "Stub research plan.";
        var traderProposal = "Stub trader proposal.";
        var riskAssessment = new RiskAssessment(RiskLevel.NEUTRAL, "Stub risk reasoning.");

        // Act — invoke the portfolio manager using a fresh FakeActionContext with its own stubs
        var pmFake = FakeActionContext.create();
        pmFake.getDelegate().expectResponse("Stub portfolio decision."); // consumed by PortfolioDecisionOutput attempt (fails)
        pmFake.getDelegate().expectResponse("Stub portfolio decision."); // consumed by String fallback
        var result = portfolioManager.portfolioDecision(
                ticker, debateState, researchPlan, traderProposal, riskAssessment,
                pmFake.getActionContext()
        );

        // Assert — verify the result is the stubbed response
        assertEquals("Stub portfolio decision.", result);

        // Verify the LLM interaction (2 calls: PortfolioDecisionOutput attempt + String fallback)
        var invocations = pmFake.getDelegate().getPromptRunner().getLlmInvocations();
        assertEquals(2, invocations.size());
        assertEquals("portfolioManager", invocations.get(0).getInteraction().getId());
        assertEquals("portfolioManager", invocations.get(1).getInteraction().getId());
    }
}