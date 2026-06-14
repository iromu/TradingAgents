package com.embabel.gekko.agent;

import com.embabel.agent.core.Agent;
import com.embabel.agent.core.AgentPlatform;
import com.embabel.gekko.config.TraderAgentConfig;
import com.embabel.gekko.web.TradingApiController;
import com.embabel.gekko.web.TradingHtmxController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests that validate agents are detected and registered in the AgentPlatform.
 * This is a Spring Boot integration test that starts the full application context.
 */
@SpringBootTest
@ActiveProfiles({"base", "app", "observability", "local"})
class AgentDetectionIntegrationTest {

    @Autowired
    private AgentPlatform agentPlatform;

    @Test
    void platformHasAgents() {
        List<Agent> agents = agentPlatform.agents();
        assertFalse(agents.isEmpty(), "AgentPlatform should have at least one agent registered");
    }

    @Test
    void platformContainsOrchestratorAgent() {
        List<Agent> agents = agentPlatform.agents();
        boolean found = agents.stream().anyMatch(a -> a.getName().equals("OrchestratorAgent"));
        assertTrue(found, "AgentPlatform should contain an agent named 'OrchestratorAgent'");
    }

    @Test
    void platformContainsDebateAgent() {
        List<Agent> agents = agentPlatform.agents();
        boolean found = agents.stream().anyMatch(a -> a.getName().equals("DebateAgent"));
        assertTrue(found, "AgentPlatform should contain an agent named 'DebateAgent'");
    }

    @Test
    void platformContainsDebateLoopAgent() {
        List<Agent> agents = agentPlatform.agents();
        boolean found = agents.stream().anyMatch(a -> a.getName().equals("DebateLoopAgent"));
        assertTrue(found, "AgentPlatform should contain an agent named 'DebateLoopAgent'");
    }

    @Test
    void platformContainsRiskDebateAgent() {
        List<Agent> agents = agentPlatform.agents();
        boolean found = agents.stream().anyMatch(a -> a.getName().equals("RiskDebateAgent"));
        assertTrue(found, "AgentPlatform should contain an agent named 'RiskDebateAgent'");
    }

    @Test
    void allFourAgentsAreRegistered() {
        List<Agent> agents = agentPlatform.agents();
        String[] expectedNames = {"OrchestratorAgent", "DebateAgent", "DebateLoopAgent", "RiskDebateAgent"};
        for (String name : expectedNames) {
            boolean found = agents.stream().anyMatch(a -> a.getName().equals(name));
            assertTrue(found, "AgentPlatform should contain an agent named '" + name + "'");
        }
    }

    @Test
    void agentCountIsAtLeastFour() {
        List<Agent> agents = agentPlatform.agents();
        assertTrue(agents.size() >= 4,
                "AgentPlatform should have at least 4 agents, but found " + agents.size());
    }

    @Test
    void orchestratorAgentHasActions() {
        List<Agent> agents = agentPlatform.agents();
        var orchestrator = agents.stream()
                .filter(a -> a.getName().equals("OrchestratorAgent"))
                .findFirst()
                .orElse(null);
        assertNotNull(orchestrator, "OrchestratorAgent should be found");
        assertFalse(orchestrator.getActions().isEmpty(),
                "OrchestratorAgent should have at least one action");
    }

    @Test
    void debateAgentHasActions() {
        List<Agent> agents = agentPlatform.agents();
        var debate = agents.stream()
                .filter(a -> a.getName().equals("DebateAgent"))
                .findFirst()
                .orElse(null);
        assertNotNull(debate, "DebateAgent should be found");
        assertFalse(debate.getActions().isEmpty(),
                "DebateAgent should have at least one action");
    }

    @Test
    void debateLoopAgentHasActions() {
        List<Agent> agents = agentPlatform.agents();
        var debateLoop = agents.stream()
                .filter(a -> a.getName().equals("DebateLoopAgent"))
                .findFirst()
                .orElse(null);
        assertNotNull(debateLoop, "DebateLoopAgent should be found");
        assertFalse(debateLoop.getActions().isEmpty(),
                "DebateLoopAgent should have at least one action");
    }

    @Test
    void riskDebateAgentHasActions() {
        List<Agent> agents = agentPlatform.agents();
        var riskDebate = agents.stream()
                .filter(a -> a.getName().equals("RiskDebateAgent"))
                .findFirst()
                .orElse(null);
        assertNotNull(riskDebate, "RiskDebateAgent should be found");
        assertFalse(riskDebate.getActions().isEmpty(),
                "RiskDebateAgent should have at least one action");
    }
}
