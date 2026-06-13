package com.embabel.gekko.htmx;

import com.embabel.agent.api.event.AgentProcessFinishedEvent;
import com.embabel.agent.api.event.AgenticEventListener;
import com.embabel.agent.api.event.AgentProcessEvent;
import com.embabel.agent.core.Agent;
import com.embabel.agent.core.AgentProcessStatusCode;
import com.embabel.gekko.htmx.HitlService.HitlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Listens to Embabel agent process lifecycle events to manage HITL sessions automatically.
 *
 * <p>Implements {@link AgenticEventListener} so it receives events directly from
 * Embabel's event system (not Spring's ApplicationEvent). The framework calls
 * {@link #onProcessEvent(AgentProcessEvent)} for every process event, and this
 * listener creates HITL sessions on failure and cleans them up on completion.
 *
 * <p>Ordered at {@value} so it runs before logging listeners, ensuring the HITL
 * session exists before any cleanup or logging logic fires.
 *
 * @see HitlService
 * @see ProcessStatusController
 */
@Component
@Order(100)
public class HitlAgenticEventListener implements AgenticEventListener {

    private static final Logger logger = LoggerFactory.getLogger(HitlAgenticEventListener.class);

    private final HitlService hitlService;

    public HitlAgenticEventListener(HitlService hitlService) {
        this.hitlService = hitlService;
    }

    /**
     * Called by Embabel's event system for every agent process event.
     * Creates HITL sessions on failure and cleans them up on completion.
     */
    @Override
    public void onProcessEvent(AgentProcessEvent event) {
        if (!(event instanceof AgentProcessFinishedEvent finished)) {
            return;
        }

        var process = finished.getAgentProcess();

        if (process.getStatus() == AgentProcessStatusCode.FAILED) {
            // Process failed — create a HITL session so the web UI can offer
            // the user a form to provide corrective input and resubmit.
            String processId = process.getId();
            String agentName = Optional.ofNullable(process.getAgent())
                    .map(Agent::getName)
                    .orElse("unknown");
            String failureInfo = Optional.ofNullable(process.getFailureInfo()).map(Object::toString).orElse("No failure details available");

            // createSession uses putIfAbsent internally — atomic check-and-create.
            // Returns the existing session if one was created concurrently.
            HitlSession session = hitlService.createSession(processId, agentName, failureInfo);
            if (!session.processId().equals(processId) || session.userActionTaken()) {
                // Another thread created a session first — log for debugging.
                logger.info("HITL session for process {} already exists (agent='{}')", processId, agentName);
            } else {
                logger.warn("Agent process {} failed for agent '{}': {} — created HITL session",
                        processId, agentName, failureInfo);
            }
        } else if (process.getStatus() == AgentProcessStatusCode.COMPLETED) {
            // Process succeeded — clean up any HITL session for this process.
            String processId = process.getId();
            hitlService.removeSession(processId);
        }
    }
}
