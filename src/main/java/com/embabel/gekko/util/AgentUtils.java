package com.embabel.gekko.util;

import com.embabel.agent.core.Agent;
import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.AgentProcess;
import com.embabel.gekko.domain.ResearchTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared utilities for agent platform operations and request handling.
 * Centralizes patterns duplicated across TradingApiController, TradingHtmxController, and ProcessStatusController.
 */
public final class AgentUtils {

    private static final Logger log = LoggerFactory.getLogger(AgentUtils.class);

    /** Default value for past_memory_str in LLM prompt models. */
    public static final String NO_PAST_MEMORY = "No past memories found.";

    private AgentUtils() {}

    /**
     * Shared lock map for process-safe WaitFor submissions.
     * Keyed by processId to avoid cross-process lock contention.
     */
    private static final Map<String, Object> PROCESS_LOCKS = new ConcurrentHashMap<>();

    /**
     * Get or create a per-process lock object.
     */
    public static Object getProcessLock(String processId) {
        return PROCESS_LOCKS.computeIfAbsent(processId, k -> new Object());
    }

    /**
     * Find the first agent matching the given class name in the platform.
     */
    public static Agent findAgent(AgentPlatform platform, Class<?> clazz) {
        String expectedName = clazz.getSimpleName();
        return platform.agents()
                .stream()
                .filter(a -> a.getName().equals(expectedName))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No " + expectedName + " found. Please ensure it is registered."
                ));
    }

    /**
     * Extract the research plan content from a process blackboard.
     */
    public static String extractPlanContent(AgentProcess process) {
        try {
            var blackboard = process.getBlackboard();
            if (blackboard == null) return null;
            List<ResearchTypes.ResearchPlan> plans = blackboard.objectsOfType(ResearchTypes.ResearchPlan.class);
            if (!plans.isEmpty()) {
                return plans.get(0).content();
            }
        } catch (Exception e) {
            log.warn("Failed to extract plan content: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Extract the investment plan content from a process blackboard.
     */
    public static String extractInvestmentPlan(AgentProcess process) {
        try {
            var blackboard = process.getBlackboard();
            if (blackboard == null) return null;
            List<ResearchTypes.InvestmentPlan> plans = blackboard.objectsOfType(ResearchTypes.InvestmentPlan.class);
            if (!plans.isEmpty()) {
                return plans.get(0).content();
            }
        } catch (Exception e) {
            log.warn("Failed to extract investment plan: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Extract the WaitFor form binding request from a process blackboard.
     * Returns empty if no form is found.
     */
    @SuppressWarnings("unchecked")
    public static java.util.Optional<com.embabel.agent.core.hitl.FormBindingRequest<?>> findWaitForForm(AgentProcess process) {
        var requests = process.getBlackboard().getObjects()
                .stream()
                .filter(com.embabel.agent.core.hitl.FormBindingRequest.class::isInstance)
                .map(o -> (com.embabel.agent.core.hitl.FormBindingRequest<?>) o)
                .toList();
        return requests.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(requests.get(0));
    }
}
