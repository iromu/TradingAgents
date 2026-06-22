package com.embabel.gekko.util;

import com.embabel.agent.core.Agent;
import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.AgentProcess;
import com.embabel.agent.core.hitl.FormBindingRequest;
import com.embabel.agent.core.hitl.FormResponse;
import com.embabel.gekko.domain.ResearchTypes;
import com.embabel.ux.form.Form;
import com.embabel.ux.form.FormSubmission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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
     * Validate a process ID as a valid UUID.
     * Extracted from TradingHtmxController and ProcessStatusController.
     */
    public static void validateProcessId(String processId) {
        try {
            UUID.fromString(processId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid process ID: " + processId);
        }
    }

    /**
     * Submit a WaitFor form and resume the agent process.
     * Returns the resumed process, or null if submission failed.
     */
    public static AgentProcess submitWaitForForm(
            AgentProcess process,
            AgentPlatform platform,
            Map<String, Object> values,
            String logPrefix
    ) {
        var formRequest = findWaitForForm(process)
                .orElseThrow(() -> new IllegalStateException("No WaitFor form found for this process."));
        var form = (Form) formRequest.getPayload();

        String submissionId = UUID.randomUUID().toString();
        FormSubmission submission = new FormSubmission(form.getId().toString(), values, submissionId, java.time.Instant.now());

        var response = new FormResponse(
                UUID.randomUUID().toString(),
                formRequest.getId().toString(),
                submission,
                false,
                java.time.Instant.now()
        );

        formRequest.onResponse(response, process);

        try {
            platform.start(process);
            return process;
        } catch (Exception e) {
            log.error("{}: {}", logPrefix, e.getMessage());
            return null;
        }
    }

    /**
     * Extract the WaitFor form binding request from a process blackboard.
     * Returns empty if no form is found.
     */
    public static Optional<FormBindingRequest<?>> findWaitForForm(AgentProcess process) {
        var blackboard = process.getBlackboard();
        if (blackboard == null) return Optional.empty();
        var requests = blackboard.getObjects()
                .stream()
                .filter(FormBindingRequest.class::isInstance)
                .map(o -> (FormBindingRequest<?>) o)
                .toList();
        return requests.isEmpty() ? Optional.empty() : Optional.of(requests.get(0));
    }
}
