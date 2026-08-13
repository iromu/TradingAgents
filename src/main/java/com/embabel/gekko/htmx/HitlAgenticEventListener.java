package com.embabel.gekko.htmx;

import com.embabel.agent.api.event.AgentProcessFinishedEvent;
import com.embabel.agent.api.event.AgenticEventListener;
import com.embabel.agent.api.event.AgentProcessEvent;
import com.embabel.agent.core.Agent;
import com.embabel.agent.core.AgentProcessStatusCode;
import com.embabel.gekko.htmx.HitlService.HitlSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Listens to Embabel agent process lifecycle events to manage HITL sessions automatically.
 * Ordered at {@value} so it runs before logging listeners.
 */
@Slf4j
@Component
@Order(100)
public class HitlAgenticEventListener implements AgenticEventListener {

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

            // createSession uses computeIfAbsent internally — atomic check-and-create.
            // If the returned session was already marked as userActionTaken, another
            // thread created it first.
            HitlSession session = hitlService.createSession(processId, agentName, failureInfo);
            if (session.userActionTaken()) {
                // Another thread created a session first — log for debugging.
                log.info("HITL session for process {} already exists (agent='{}')", processId, agentName);
            } else {
                log.warn("Agent process {} failed for agent '{}': {} — created HITL session",
                        processId, agentName, failureInfo);
            }
        } else if (process.getStatus() == AgentProcessStatusCode.COMPLETED) {
            // Process succeeded — clean up any HITL session for this process.
            String processId = process.getId();
            hitlService.removeSession(processId);
        }
    }
}
