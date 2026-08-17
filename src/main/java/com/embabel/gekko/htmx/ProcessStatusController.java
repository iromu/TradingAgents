package com.embabel.gekko.htmx;

import com.embabel.agent.core.Agent;
import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.AgentProcess;
import com.embabel.agent.core.AgentProcessStatusCode;
import com.embabel.agent.core.hitl.FormBindingRequest;
import com.embabel.ux.form.Form;
import com.embabel.gekko.domain.ResearchTypes;
import com.embabel.gekko.htmx.GenericProcessingValues;
import com.embabel.gekko.htmx.HitlService.HitlSession;
import com.embabel.gekko.util.AgentUtils;
import com.embabel.gekko.web.ResearchPlanService;
import com.embabel.gekko.web.TradingHtmxController.TickerForm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpSession;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Handles HITL (Human-in-the-Loop) workflow for failed agent processes.
 */
@Slf4j
@Controller
public class ProcessStatusController {

    /** HTTP session attribute that binds a processId to the user's session. */
    private static final String SESSION_PROCESS_ID = "hitl_processId";

    private static final java.util.regex.Pattern TICKER_PATTERN = java.util.regex.Pattern.compile("^[A-Z0-9.]+$");

    private final AgentPlatform agentPlatform;
    private final HitlService hitlService;
    private final ResearchPlanService researchPlanService;

    public ProcessStatusController(AgentPlatform agentPlatform, HitlService hitlService, ResearchPlanService researchPlanService) {
        this.agentPlatform = agentPlatform;
        this.hitlService = hitlService;
        this.researchPlanService = researchPlanService;
    }

    @GetMapping("/status/{processId}")
    public String checkPlanStatus(
            @PathVariable String processId,
            @RequestParam String resultModelKey,
            @RequestParam String successView,
            HttpSession session,
            Model model
    ) {
        AgentUtils.validateProcessId(processId);
        var agentProcess = agentPlatform.getAgentProcess(processId);
        if (agentProcess == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Process not found");
        }

        AgentProcessStatusCode status = agentProcess.getStatus();

        switch (status) {
            case COMPLETED -> {
                log.info("Process {} completed successfully", processId);
                Object result = agentProcess.lastResult();
                model.addAttribute(resultModelKey, result);
                model.addAttribute("agentProcess", agentProcess);
                return successView;
            }

            case FAILED -> {
                log.error("Process {} failed", processId);

                // The HitlAgenticEventListener should have already created a session via
                // AgentProcessFinishedEvent. Show the form if it exists.
                HitlSession hitlSession = hitlService.getSession(processId).orElse(null);
                if (hitlSession == null) {
                    // Safety net: event listener may not have fired (e.g., profile mismatch).
                    // Create the session so the user still gets the HITL form.
                    String agentName = Optional.ofNullable(agentProcess.getAgent())
                            .map(Agent::getName)
                            .orElse("unknown");
                    String failureInfo = Optional.ofNullable(agentProcess.getFailureInfo())
                            .map(Object::toString)
                            .orElse("No failure details available");
                    log.warn("No HITL session for failed process {} (agent='{}') — creating defensively",
                            processId, agentName);
                    hitlSession = hitlService.createSession(processId, agentName, failureInfo);
                }

                // Bind processId to HTTP session for ownership verification
                session.setAttribute(SESSION_PROCESS_ID, processId);

                model.addAttribute("hitlSession", hitlSession);
                model.addAttribute("processId", processId);
                model.addAttribute("pageTitle", "Human Review Required");
                return "common/hitl";
            }

            case TERMINATED -> {
                log.info("Process {} was terminated", processId);
                model.addAttribute("error", "Process was terminated before completion");
                return "common/processing-error";
            }

            case WAITING -> {
                log.info("Process {} is waiting for human input (HITL WaitFor checkpoint)", processId);
                // Bind processId to HTTP session for ownership verification
                session.setAttribute(SESSION_PROCESS_ID, processId);
                return renderWaitingForm(processId, agentProcess, model);
            }

            default -> {
                model.addAttribute("processId", processId);
                model.addAttribute("pageTitle", "Planning...");
                return "common/processing";
            }
        }
    }

    @PostMapping("/status/{processId}/resubmit")
    public String resubmit(
            @PathVariable String processId,
            @RequestParam(required = false, defaultValue = "") String userInput,
            @RequestParam(required = false, defaultValue = "") String feedback,
            HttpSession session,
            Model model
    ) {
        AgentUtils.validateProcessId(processId);

        // Verify process ownership: processId must match the one bound to this HTTP session
        String ownedProcessId = (String) session.getAttribute(SESSION_PROCESS_ID);
        if (ownedProcessId == null || !ownedProcessId.equals(processId)) {
            log.warn("Process ownership violation: session had '{}', request for '{}'", ownedProcessId, processId);
            model.addAttribute("error", "Access denied: process not associated with this session.");
            model.addAttribute("pageTitle", "Access Denied");
            return "common/processing-error";
        }

        if (feedback.length() > 10000) {
            model.addAttribute("error", "Feedback must not exceed 10,000 characters");
            model.addAttribute("pageTitle", "Validation Error");
            return "common/processing-error";
        }
        if (userInput == null || userInput.isBlank() || !TICKER_PATTERN.matcher(userInput).matches()) {
            model.addAttribute("error", "Invalid ticker format. Expected uppercase letters, digits, and dots only (e.g. AAPL, BRK.B)");
            model.addAttribute("pageTitle", "Validation Error");
            return "common/processing-error";
        }
        // Check if already resolved — prevent duplicate resubmissions
        // Use per-process lock to avoid blocking other processes
        AgentProcess agentProcess;
        synchronized (AgentUtils.getProcessLock(processId)) {
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
            agentProcess = researchPlanService.createAndStart(agent, form);

            // Start the process BEFORE migrating the session.
            // If start() fails, the old session is still intact and the user can retry.
            try {
                agentPlatform.start(agentProcess);
            } catch (Exception e) {
                log.error("Failed to start retry process for HITL session {}", processId, e);
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

    private String renderWaitingForm(String processId, AgentProcess agentProcess, Model model) {
        var blackboard = agentProcess.getBlackboard();
        if (blackboard == null) {
            log.warn("Blackboard is null for process {} — rendering waiting form without debate preview", processId);
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

    @PostMapping("/status/{processId}/waitfor")
    public String submitWaitForFeedback(
            @PathVariable String processId,
            @RequestParam(required = false, defaultValue = "") String feedback,
            @RequestParam(required = false, defaultValue = "false") boolean approved,
            HttpSession session,
            Model model
    ) {
        AgentUtils.validateProcessId(processId);

        // Verify process ownership: processId must match the one bound to this HTTP session
        String ownedProcessId = (String) session.getAttribute(SESSION_PROCESS_ID);
        if (ownedProcessId == null || !ownedProcessId.equals(processId)) {
            log.warn("Process ownership violation: session had '{}', request for '{}'", ownedProcessId, processId);
            model.addAttribute("error", "Access denied: process not associated with this session.");
            model.addAttribute("pageTitle", "Access Denied");
            return "common/processing-error";
        }

        if (feedback.length() > 10000) {
            model.addAttribute("error", "Feedback must not exceed 10,000 characters");
            model.addAttribute("pageTitle", "Validation Error");
            return "common/processing-error";
        }
        AgentProcess agentProcess = agentPlatform.getAgentProcess(processId);
        if (agentProcess == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Process not found");
        }

        synchronized (AgentUtils.getProcessLock(processId)) {
            // Check process is still WAITING — prevent duplicate submissions
            if (agentProcess.getStatus() != AgentProcessStatusCode.WAITING) {
                model.addAttribute("error", "Process is no longer in WAITING state. It may have already been resumed or failed.");
                model.addAttribute("pageTitle", "Process Not Waiting");
                return "common/processing-error";
            }

            Map<String, Object> values = Map.of("approved", approved, "feedback", feedback);
            var resumed = AgentUtils.submitWaitForForm(agentProcess, agentPlatform, values, "WaitFor submission");
            if (resumed == null) {
                model.addAttribute("error", "Failed to resume process: " + processId);
                model.addAttribute("pageTitle", "Resume Failed");
                return "common/processing-error";
            }

            log.info("WaitFor form submitted for process {}, resuming...", processId);
        }

        model.addAttribute("processId", agentProcess.getId());
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
