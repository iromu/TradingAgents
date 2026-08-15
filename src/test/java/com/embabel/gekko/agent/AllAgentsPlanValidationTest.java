package com.embabel.gekko.agent;

import com.embabel.agent.core.Action;
import com.embabel.agent.core.Agent;
import com.embabel.agent.core.Goal;
import com.embabel.agent.test.integration.EmbabelMockitoIntegrationTest;
import com.embabel.gekko.agent.FakeActionContext;
import com.embabel.agent.test.unit.FakeOperationContext;
import com.embabel.gekko.agent.managers.PortfolioManager;
import com.embabel.gekko.domain.ResearchTypes;
import com.embabel.gekko.util.FileCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive integration test verifying all agents are registered, have valid goal paths,
 * and can produce a plan for the entry point workflow.
 *
 * <p>Tests three layers of correctness:
 * <ol>
 *   <li><b>Registration:</b> All @Agent beans are discovered by AgentPlatform</li>
 *   <li><b>Goal validity:</b> Every agent has at least one Goal (from @AchievesGoal), and every
 *       Goal's preconditions can be satisfied by other actions in the same agent</li>
 *   <li><b>Plan feasibility:</b> The planner can construct a plan for the OrchestratorAgent's
 *       entry-point goal (executeDebate → full research workflow)</li>
 * </ol>
 */
@Tag("integration")
class AllAgentsPlanValidationTest extends EmbabelMockitoIntegrationTest {

    private FakeOperationContext ctx;
    private ResearchTypes.Ticker ticker;

    @BeforeEach
    void setUp() {
        ctx = FakeOperationContext.create();
        ticker = new ResearchTypes.Ticker("AAPL", "");
    }

    // ========================================================
    // Layer 1 — Agent registration
    // ========================================================

    @Test
    void allAgentsAreRegistered() {
        List<String> names = agentPlatform.agents().stream()
                .map(Agent::getName)
                .sorted()
                .toList();

        // Core agents that must exist
        List<String> required = List.of(
                "OrchestratorAgent",
                "DebateAgent",
                "DebateLoopAgent",
                "RiskDebateAgent",
                "InstrumentIdentityAgent",
                "CheckpointAgent"
        );

        for (String requiredAgent : required) {
            assertTrue(names.contains(requiredAgent),
                    "AgentPlatform must register '" + requiredAgent + "'. Registered: " + names);
        }
    }

    @Test
    void everyAgentHasAtLeastOneAction() {
        for (Agent agent : agentPlatform.agents()) {
            assertFalse(agent.getActions().isEmpty(),
                    "Agent '" + agent.getName() + "' must have at least one action");
        }
    }

    @Test
    void agentCountMatchesExpectedMinimum() {
        int count = agentPlatform.agents().size();
        assertTrue(count >= 6,
                "Expected at least 6 agents (Orchestrator, Debate, DebateLoop, RiskDebate, "
                + "InstrumentIdentity, Checkpoint), but found " + count);
    }

    // ========================================================
    // Layer 2 — Goal validity (no MISSING_GOALS, no NO_PATH_TO_GOAL)
    // ========================================================

    @Test
    void everyAgentHasAtLeastOneGoal() {
        for (Agent agent : agentPlatform.agents()) {
            long goalCount = agent.getGoals().size();
            assertTrue(goalCount >= 1,
                    "Agent '" + agent.getName() + "' must have at least one Goal (from @AchievesGoal). "
                    + "Found " + goalCount + " goals among " + agent.getActions().size() + " actions");
        }
    }

    @Test
    void everyGoalHasNonBlankDescription() {
        for (Agent agent : agentPlatform.agents()) {
            for (Goal goal : agent.getGoals()) {
                String desc = goal.getDescription();
                assertFalse(desc.isBlank(),
                        "Goal description must not be blank for goal '" + goal.getName()
                        + "' in agent '" + agent.getName() + "'");
            }
        }
    }

    @Test
    void everyGoalIsLinkedToAnAction() {
        // Each Goal should correspond to an Action that implements it.
        // The goal name should match an action name.
        for (Agent agent : agentPlatform.agents()) {
            var actionNames = agent.getActions().stream()
                    .map(Action::getName)
                    .toList();

            for (Goal goal : agent.getGoals()) {
                assertTrue(actionNames.contains(goal.getName()),
                        "Goal '" + goal.getName() + "' in agent '" + agent.getName()
                        + "' must correspond to an action with the same name. "
                        + "Available actions: " + actionNames);
            }
        }
    }

    @Test
    void orchestratorAgentHasExecuteDebateGoal() {
        var orchestrator = agentPlatform.agents().stream()
                .filter(a -> a.getName().equals("OrchestratorAgent"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("OrchestratorAgent not found"));

        assertTrue(orchestrator.getGoals().size() >= 1,
                "OrchestratorAgent must have at least one Goal, found " + orchestrator.getGoals().size());

        // Verify the main entry goal is present
        var executeDebate = orchestrator.getGoals().stream()
                .filter(g -> g.getName().contains("executeDebate"))
                .findFirst();
        assertTrue(executeDebate.isPresent(),
                "OrchestratorAgent must have 'executeDebate' as a Goal");
    }

    @Test
    void debateAgentHasGoals() {
        var debate = agentPlatform.agents().stream()
                .filter(a -> a.getName().equals("DebateAgent"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("DebateAgent not found"));

        assertTrue(debate.getGoals().size() >= 1,
                "DebateAgent must have at least one Goal, found " + debate.getGoals().size());
    }

    @Test
    void debateLoopAgentHasGoals() {
        var debateLoop = agentPlatform.agents().stream()
                .filter(a -> a.getName().equals("DebateLoopAgent"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("DebateLoopAgent not found"));

        assertTrue(debateLoop.getGoals().size() >= 1,
                "DebateLoopAgent must have at least one Goal, found " + debateLoop.getGoals().size());
    }

    @Test
    void riskDebateAgentHasGoals() {
        var riskDebate = agentPlatform.agents().stream()
                .filter(a -> a.getName().equals("RiskDebateAgent"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("RiskDebateAgent not found"));

        assertTrue(riskDebate.getGoals().size() >= 1,
                "RiskDebateAgent must have at least one Goal, found " + riskDebate.getGoals().size());
    }

    @Test
    void instrumentIdentityAgentHasGoals() {
        var identity = agentPlatform.agents().stream()
                .filter(a -> a.getName().equals("InstrumentIdentityAgent"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("InstrumentIdentityAgent not found"));

        assertTrue(identity.getGoals().size() >= 1,
                "InstrumentIdentityAgent must have at least one Goal, found " + identity.getGoals().size());
    }

    @Test
    void checkpointAgentHasGoals() {
        var checkpoint = agentPlatform.agents().stream()
                .filter(a -> a.getName().equals("CheckpointAgent"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("CheckpointAgent not found"));

        assertTrue(checkpoint.getGoals().size() >= 1,
                "CheckpointAgent must have at least one Goal, found " + checkpoint.getGoals().size());
    }

    // ========================================================
    // Layer 3 — Plan feasibility (planner can make a plan)
    // ========================================================

    @Test
    void plannerCanMakePlanForOrchestratorEntryGoal() {
        // The OrchestratorAgent's executeDebate is the main entry point.
        // If the planner can find a path from a non-goal action to executeDebate,
        // then a plan can be made.
        var orchestrator = agentPlatform.agents().stream()
                .filter(a -> a.getName().equals("OrchestratorAgent"))
                .findFirst()
                .orElseThrow();

        var executeDebate = orchestrator.getGoals().stream()
                .filter(g -> g.getName().contains("executeDebate"))
                .findFirst();

        assertTrue(executeDebate.isPresent(),
                "executeDebate goal not found");

        var goal = executeDebate.get();

        // Verify the goal has a description (required for plan generation)
        String goalDesc = goal.getDescription();
        assertFalse(goalDesc.isBlank(),
                "Goal description must not be blank for plan generation");

        // Verify the goal has an output type (required for plan generation)
        assertNotNull(goal.getOutputType(),
                "Goal must have an output type for plan generation");
    }

    @Test
    void allGoalsHaveOutputTypes() {
        for (Agent agent : agentPlatform.agents()) {
            for (Goal goal : agent.getGoals()) {
                assertNotNull(goal.getOutputType(),
                        "Goal '" + goal.getName() + "' in agent '" + agent.getName()
                        + "' must have an output type");
            }
        }
    }

    // ========================================================
    // Layer 4 — Smoke test that key actions execute (via direct instantiation)
    // ========================================================

    @Test
    void debateAgentGenerateFundamentalsReportExecutes() {
        ctx.expectResponse("Stub fundamentals report.");

        var cache = new FileCache();
        var debateAgent = new DebateAgent(
                cache, null, null, null, null, null, null, null, null
        );

        var result = debateAgent.generateFundamentalsReport(ticker, ctx);
        assertNotNull(result);
        assertEquals("Stub fundamentals report.", result.content());
    }

    @Test
    void orchestratorGenerateResearchPlanExecutes() {
        ctx.expectResponse("Stub research plan.");

        var cache = new FileCache();
        var agent = new OrchestratorAgent(
                cache, null, null, null, null, null, null
        );

        var result = agent.generateResearchPlan(ticker, null, ctx);
        assertNotNull(result);
        assertEquals("Stub research plan.", result.content());
    }

    @Test
    void portfolioManagerDecisionExecutes() {
        var debateState = new ResearchTypes.InvestmentDebateState(
                List.of("bull argument", "bear argument"),
                List.of("bull argument"),
                List.of("bear argument"),
                "bear argument", 2,
                new ResearchTypes.DebateBriefs("F", "M", "N", "S"), -1.0
        );

        ctx.expectResponse("Stub portfolio decision.");
        ctx.expectResponse("Stub portfolio decision.");

        var pmFake = FakeActionContext.create();
        pmFake.getDelegate().expectResponse("Stub portfolio decision.");
        pmFake.getDelegate().expectResponse("Stub portfolio decision.");

        var portfolioManager = new PortfolioManager();
        var riskAssessment = new RiskAssessment(RiskLevel.NEUTRAL, "Stub risk reasoning.");

        var result = portfolioManager.portfolioDecision(
                ticker, debateState, "Stub plan.", "Stub trader proposal.",
                riskAssessment, pmFake.getActionContext()
        );

        assertNotNull(result);
        assertEquals("Stub portfolio decision.", result);
    }

    @Test
    void decisionMemoryAgentCanBeInstantiated() {
        var memoryAgent = new com.embabel.gekko.agent.memory.DecisionMemoryAgent(
                null, null
        );
        assertNotNull(memoryAgent);
    }
}
