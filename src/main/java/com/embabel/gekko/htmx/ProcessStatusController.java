package com.embabel.gekko.htmx;

import com.embabel.agent.core.Agent;
import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.AgentProcess;
import com.embabel.agent.core.AgentProcessStatusCode;
import com.embabel.agent.core.Budget;
import com.embabel.agent.core.ProcessOptions;
import com.embabel.agent.core.Verbosity;
import com.embabel.agent.core.hitl.Awaitable;
import com.embabel.agent.core.hitl.FormBindingRequest;
import com.embabel.agent.core.hitl.FormResponse;
import com.embabel.ux.form.Control;
import com.embabel.ux.form.Form;
import com.embabel.ux.form.FormSubmission;
import com.embabel.ux.form.SimpleFormGenerator;
import com.embabel.gekko.domain.ResearchTypes;
import com.embabel.gekko.htmx.GenericProcessingValues;
import com.embabel.gekko.htmx.HitlService.HitlSession;
import com.embabel.gekko.web.TradingHtmxController.TickerForm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles HITL (Human-in-the-Loop) workflow for failed agent processes.
 *
 * <p>Flow:
 * <ol>
 *   <li>Agent process fails → {@link HitlAgenticEventListener} creates a HITL session</li>
 *   <li>User polls {@code /status/{processId}} → controller detects FAILED status and shows HITL form</li>
 *   <li>User submits feedback → {@code /resubmit} creates a new agent process with feedback injected</li>
 *   <li>Session is migrated to the new processId so the retry is tracked</li>
 * </ol>
 */
@Controller
public class ProcessStatusController {

    private static final Logger logger = LoggerFactory.getLogger(ProcessStatusController.class);

    private final AgentPlatform agentPlatform;
    private final HitlService hitlService;

    public ProcessStatusController(AgentPlatform agentPlatform, HitlService hitlService) {
        this.agentPlatform = agentPlatform;
        this.hitlService = hitlService;
    }

    /**
     * Poll endpoint for the processing page. Returns different views based on process status.
     */
    @GetMapping("/status/{processId}")
    public String checkPlanStatus(
            @PathVariable String processId,
            @RequestParam String resultModelKey,
            @RequestParam String successView,
            Model model
    ) {

        var agentProcess = agentPlatform.getAgentProcess(processId);
        if (agentProcess == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Process not found");
        }

        AgentProcessStatusCode status = agentProcess.getStatus();

        switch (status) {
            case COMPLETED -> {
                logger.info("Process {} completed successfully", processId);
                Object result = agentProcess.lastResult();
                model.addAttribute(resultModelKey, result);
                model.addAttribute("agentProcess", agentProcess);
                return successView;
            }

            case FAILED -> {
                logger.error("Process {} failed", processId);

                // The HitlAgenticEventListener should have already created a session via
                // AgentProcessFinishedEvent. Show the form if it exists.
                HitlSession session = hitlService.getSession(processId).orElse(null);
                if (session == null) {
                    // Safety net: event listener may not have fired (e.g., profile mismatch).
                    // Create the session so the user still gets the HITL form.
                    String agentName = Optional.ofNullable(agentProcess.getAgent())
                            .map(Agent::getName)
                            .orElse("unknown");
                    String failureInfo = Optional.ofNullable(agentProcess.getFailureInfo())
                            .map(Object::toString)
                            .orElse("No failure details available");
                    logger.warn("No HITL session for failed process {} (agent='{}') — creating defensively",
                            processId, agentName);
                    session = hitlService.createSession(processId, agentName, failureInfo);
                }

                model.addAttribute("hitlSession", session);
                model.addAttribute("processId", processId);
                model.addAttribute("pageTitle", "Human Review Required");
                return "common/hitl";
            }

            case TERMINATED -> {
                logger.info("Process {} was terminated", processId);
                model.addAttribute("error", "Process was terminated before completion");
                return "common/processing-error";
            }

            case WAITING -> {
                logger.info("Process {} is waiting for human input (HITL WaitFor checkpoint)", processId);
                return renderWaitingForm(processId, agentProcess, model);
            }

            default -> {
                model.addAttribute("processId", processId);
                model.addAttribute("pageTitle", "Planning...");
                return "common/processing";
            }
        }
    }

    /**
     * Submit human input/feedback for a failed process and restart it.
     *
     * <p>Resubmit flow:
     * <ol>
     *   <li>Check resolved flag — prevent duplicate submissions</li>
     *   <li>Look up the agent by name stored in the HITL session (deterministic)</li>
     *   <li>Create the new agent process with feedback injected</li>
     *   <li>Start the process — if this fails, the old session is intact</li>
     *   <li>Migrate the HITL session to the new processId (point of no return)</li>
     * </ol>
     */
    @PostMapping("/status/{processId}/resubmit")
    public String resubmit(
            @PathVariable String processId,
            @RequestParam(required = false, defaultValue = "") String userInput,
            @RequestParam(required = false, defaultValue = "") String feedback,
            Model model
    ) {
        // Check if already resolved — prevent duplicate resubmissions
        // Synchronized with updateSession to prevent race conditions
        AgentProcess agentProcess;
        synchronized (hitlService) {
            HitlSession existingSession = hitlService.getSession(processId).orElse(null);
            if (existingSession == null) {
                model.addAttribute("error", "No HITL session found for process " + processId);
                model.addAttribute("pageTitle", "Session Not Found");
                return "common/processing-error";
            }
            if (existingSession.userActionTaken()) {
                model.addAttribute("error", "This process has already been resubmitted. Please wait for the retry to complete.");
                model.addAttribute("pageTitle", "Already Resubmitted");
                return "common/processing-error";
            }

            // Look up the agent by name stored in the HITL session (deterministic lookup)
            String agentName = existingSession.agentName();
            var agent = agentPlatform.agents()
                    .stream()
                    .filter(a -> a.getName().equals(agentName))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "Agent '" + agentName + "' not found — process " + processId + " cannot be retried"));

            // Create a new TickerForm with feedback injected into the agent
            var form = new TickerForm(userInput, feedback);
            agentProcess = agentPlatform.createAgentProcessFrom(
                    agent,
                    ProcessOptions.DEFAULT
                            .withVerbosity(new Verbosity(true, true))
                            .withBudget(new Budget().withTokens(16384)),
                    form
            );

            // Start the process BEFORE migrating the session.
            // If start() fails, the old session is still intact and the user can retry.
            try {
                agentPlatform.start(agentProcess);
            } catch (Exception e) {
                logger.error("Failed to start retry process for HITL session {}", processId, e);
                model.addAttribute("error", "Failed to start retry: " + e.getMessage());
                model.addAttribute("pageTitle", "Retry Failed");
                return "common/processing-error";
            }

            // Now that the process started successfully, migrate the session to the new processId.
            // This is the point of no return — the retry is running.
            hitlService.updateSession(processId, userInput, feedback, agentProcess.getId());
        }

        model.addAttribute("processId", agentProcess.getId());
        model.addAttribute("pageTitle", "Planning your research (retry)");
        new GenericProcessingValues(
                agentProcess,
                "Planning your research (retry)",
                userInput,
                "researchPlan",
                "plan"
        ).addToModel(model);

        return "common/processing";
    }

    /**
     * Renders the WaitFor HITL form for a process in WAITING state.
     *
     * <p>Extracts the InvestmentDebateState from the blackboard and the
     * FormBindingRequest (generated by WaitFor.formSubmission()) to pre-populate
     * the form with debate context and form metadata.
     */
    private String renderWaitingForm(String processId, AgentProcess agentProcess, Model model) {
        var blackboard = agentProcess.getBlackboard();
        if (blackboard == null) {
            logger.warn("Blackboard is null for process {} — rendering waiting form without debate preview", processId);
            model.addAttribute("processId", processId);
            model.addAttribute("pageTitle", "Review & Approve Investment Plan");
            model.addAttribute("debateHistory", List.of());
            model.addAttribute("formId", null);
            model.addAttribute("formTitle", "Investment Review");
            return "common/waiting";
        }

        // Use bullHistory and bearHistory directly instead of reconstructing from the flat history list
        List<ResearchTypes.InvestmentDebateState> debateStates = blackboard.objectsOfType(ResearchTypes.InvestmentDebateState.class);
        List<Map<String, Object>> debateHistory = new ArrayList<>();
        if (!debateStates.isEmpty()) {
            ResearchTypes.InvestmentDebateState state = debateStates.get(0);
            List<String> bullHistory = state.bullHistory();
            List<String> bearHistory = state.bearHistory();
            int maxTurns = Math.max(bullHistory.size(), bearHistory.size());
            for (int i = 0; i < maxTurns; i++) {
                if (i < bullHistory.size()) {
                    Map<String, Object> turn = new LinkedHashMap<>();
                    turn.put("bull", true);
                    turn.put("text", bullHistory.get(i));
                    debateHistory.add(turn);
                }
                if (i < bearHistory.size()) {
                    Map<String, Object> turn = new LinkedHashMap<>();
                    turn.put("bull", false);
                    turn.put("text", bearHistory.get(i));
                    debateHistory.add(turn);
                }
            }
        }

        // Extract the generated form from the FormBindingRequest on the blackboard
        @SuppressWarnings("unchecked")
        List<FormBindingRequest<?>> requests = (List) agentProcess.getBlackboard().getObjects()
                .stream()
                .filter(FormBindingRequest.class::isInstance)
                .map(o -> (FormBindingRequest<?>) o)
                .toList();
        String formId = null;
        String formTitle = null;
        if (!requests.isEmpty()) {
            FormBindingRequest<?> request = requests.get(0);
            Form form = (Form) request.getPayload();
            formId = form.getId().toString();
            formTitle = form.getTitle();
        }

        model.addAttribute("processId", processId);
        model.addAttribute("pageTitle", "Review & Approve Investment Plan");
        model.addAttribute("debateHistory", debateHistory);
        model.addAttribute("formId", formId);
        model.addAttribute("formTitle", formTitle != null ? formTitle : "Investment Review");

        return "common/waiting";
    }

    /**
     * Submits the WaitFor HITL form and resumes the agent process.
     *
     * <p>Flow:
     * <ol>
     *   <li>Find the FormBindingRequest on the blackboard</li>
     *   <li>Build a FormSubmission from the form fields</li>
     *   <li>Create a FormResponse and call FormBindingRequest.onResponse()</li>
     *   <li>Resume the process by calling agentPlatform.start()</li>
     * </ol>
     */
    @PostMapping("/status/{processId}/waitfor")
    public String submitWaitForFeedback(
            @PathVariable String processId,
            @RequestParam(required = false, defaultValue = "") String feedback,
            @RequestParam(required = false, defaultValue = "false") boolean approved,
            Model model
    ) {
        AgentProcess agentProcess = agentPlatform.getAgentProcess(processId);
        if (agentProcess == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Process not found");
        }

        synchronized (agentProcess) {
            // Check process is still WAITING — prevent duplicate submissions
            if (agentProcess.getStatus() != AgentProcessStatusCode.WAITING) {
                model.addAttribute("error", "Process is no longer in WAITING state. It may have already been resumed or failed.");
                model.addAttribute("pageTitle", "Process Not Waiting");
                return "common/processing-error";
            }

            // Find the FormBindingRequest on the blackboard
            @SuppressWarnings("unchecked")
            List<FormBindingRequest<?>> requests = (List) agentProcess.getBlackboard().getObjects()
                    .stream()
                    .filter(FormBindingRequest.class::isInstance)
                    .map(o -> (FormBindingRequest<?>) o)
                    .toList();
            if (requests.isEmpty()) {
                model.addAttribute("error", "No WaitFor form found for this process.");
                model.addAttribute("pageTitle", "Form Not Found");
                return "common/processing-error";
            }

            FormBindingRequest<?> request = requests.get(0);
            Form form = (Form) request.getPayload();

            // Build the form submission values map
            // Control IDs match the record field names: "feedback", "approved"
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("feedback", feedback);
            values.put("approved", approved);

            String submissionId = UUID.randomUUID().toString();
            FormSubmission submission = new FormSubmission(form.getId().toString(), values, submissionId, java.time.Instant.now());

            // Create the form response with the awaitable ID
            FormResponse response = new FormResponse(
                    UUID.randomUUID().toString(),
                    request.getId().toString(),
                    submission,
                    false,
                    java.time.Instant.now()
            );

            // Process the form response — this binds the result to the blackboard
            request.onResponse(response, agentProcess);

            // Resume the process INSIDE the synchronized block to prevent the race condition
            // where another thread could call start() between onResponse() and start().
            try {
                agentPlatform.start(agentProcess);
            } catch (Exception e) {
                logger.error("Failed to resume process {} after WaitFor submission", processId, e);
                model.addAttribute("error", "Failed to resume process: " + e.getMessage());
                model.addAttribute("pageTitle", "Resume Failed");
                return "common/processing-error";
            }

            logger.info("WaitFor form submitted for process {}, resuming...", processId);
        }

        model.addAttribute("processId", processId);
        model.addAttribute("pageTitle", "Investment Research in Progress");
        new GenericProcessingValues(
                agentProcess,
                "Investment research in progress",
                feedback,
                "investmentPlan",
                "plan"
        ).addToModel(model);

        return "common/processing";
    }
}
