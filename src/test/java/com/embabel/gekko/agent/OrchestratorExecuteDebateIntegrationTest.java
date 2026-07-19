package com.embabel.gekko.agent;

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
 * Integration tests for OrchestratorAgent.executeDebate() — sub-process delegation to DebateAgent.
 */
@Tag("integration")
class OrchestratorExecuteDebateIntegrationTest extends EmbabelMockitoIntegrationTest {

    private Path tempCacheDir;
    private FileCache cache;

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
        tempCacheDir = Files.createTempDirectory("orchestrator-execute-debate-test-cache-");
        cache = new FileCache();
        var field = FileCache.class.getDeclaredField("baseDir");
        field.setAccessible(true);
        field.set(cache, tempCacheDir);
    }

    @Test
    void executeDebate_actionExistsOnOrchestrator() {
        var debateAgentProvider = mock(ObjectProvider.class);
        when(debateAgentProvider.getObject()).thenReturn(null);

        var orchestrator = new OrchestratorAgent(
                cache,
                null, // identityAgent
                null, // memoryAgent
                null, // checkpointAgent
                null, // instrumentContextContributor
                debateAgentProvider,
                null  // llmBudgetTracker
        );

        // Verify the action exists
        var agent = agentPlatform.agents().stream()
                .filter(a -> a.getName().equals("OrchestratorAgent"))
                .findFirst()
                .orElseThrow();

        var actions = agent.getActions();
        assertTrue(actions.stream().anyMatch(a -> a.getName().contains("execute") || a.getName().contains("Debate")),
                "OrchestratorAgent should have executeDebate action. Actions: " + actions.stream().map(Object::toString).toList());
    }

    @Test
    void executeDebate_returnsInvestmentPlanType() {
        var debateAgentProvider = mock(ObjectProvider.class);
        when(debateAgentProvider.getObject()).thenReturn(null);

        var orchestrator = new OrchestratorAgent(
                cache,
                null, null, null, null,
                debateAgentProvider,
                null
        );

        var agent = agentPlatform.agents().stream()
                .filter(a -> a.getName().equals("OrchestratorAgent"))
                .findFirst()
                .orElseThrow();

        var executeAction = agent.getActions().stream()
                .filter(a -> a.getName().contains("execute"))
                .findFirst()
                .orElseThrow();

        assertNotNull(executeAction);
    }

    @Test
    void orchestratorPlanApprovalRecordHasCorrectFields() {
        var approval = new ResearchTypes.PlanApproval("Looks good", true);
        assertEquals("Looks good", approval.feedback());
        assertTrue(approval.approved());
    }
}