package com.embabel.gekko.web;

import com.embabel.agent.core.Agent;
import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.AgentProcess;
import com.embabel.agent.core.AgentProcessStatusCode;
import com.embabel.agent.core.Budget;
import com.embabel.agent.core.ProcessOptions;
import com.embabel.agent.core.Verbosity;
import com.embabel.gekko.agent.OrchestratorAgent;
import com.embabel.gekko.util.AgentUtils;
import org.springframework.stereotype.Service;

/**
 * Shared service for creating and managing agent research processes.
 * Extracted from duplicated code across TradingApiController, TradingHtmxController, and ProcessStatusController.
 */
@Service
public class ResearchPlanService {

    private final AgentPlatform agentPlatform;

    public ResearchPlanService(AgentPlatform agentPlatform) {
        this.agentPlatform = agentPlatform;
    }

    /**
     * Create and start a new agent process for research planning.
     *
     * @param input The input object (Ticker or TickerForm) to pass to the agent.
     * @return The created and started AgentProcess.
     */
    public AgentProcess createAndStart(Object input) {
        var agent = AgentUtils.findAgent(agentPlatform, OrchestratorAgent.class);
        var agentProcess = agentPlatform.createAgentProcessFrom(
                agent,
                ProcessOptions.DEFAULT
                        .withVerbosity(new Verbosity(true, true))
                        .withBudget(new Budget().withTokens(16384)),
                input
        );
        agentPlatform.start(agentProcess);
        return agentProcess;
    }

    /**
     * Create and start a new agent process using a specific agent (e.g. for HITL retry).
     *
     * @param agent The agent to use.
     * @param input The input object to pass to the agent.
     * @return The created and started AgentProcess.
     */
    public AgentProcess createAndStart(Agent agent, Object input) {
        var agentProcess = agentPlatform.createAgentProcessFrom(
                agent,
                ProcessOptions.DEFAULT
                        .withVerbosity(new Verbosity(true, true))
                        .withBudget(new Budget().withTokens(16384)),
                input
        );
        agentPlatform.start(agentProcess);
        return agentProcess;
    }

    /**
     * Get the current status of an agent process.
     *
     * @param processId The process ID.
     * @return The AgentProcess, or null if not found.
     */
    public AgentProcess getProcess(String processId) {
        return agentPlatform.getAgentProcess(processId);
    }

    /**
     * Check if a process is in WAITING state.
     *
     * @param processId The process ID.
     * @return true if the process is WAITING.
     */
    public boolean isWaiting(String processId) {
        var process = agentPlatform.getAgentProcess(processId);
        return process != null && process.getStatus() == AgentProcessStatusCode.WAITING;
    }

    /**
     * Submit a WaitFor form and resume the process.
     *
     * @param process The process to resume.
     * @param values The form values to submit.
     * @param errorMsg The error message to use on failure.
     * @return The resumed AgentProcess, or null on failure.
     */
    public AgentProcess submitWaitForForm(AgentProcess process, java.util.Map<String, Object> values, String errorMsg) {
        return AgentUtils.submitWaitForForm(process, agentPlatform, values, errorMsg);
    }
}