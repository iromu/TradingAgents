package com.embabel.gekko.web;

import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.AgentProcess;
import com.embabel.agent.core.AgentProcessStatusCode;
import com.embabel.agent.core.Budget;
import com.embabel.agent.core.ProcessOptions;
import com.embabel.agent.core.Verbosity;
import com.embabel.agent.core.hitl.FormBindingRequest;
import com.embabel.agent.core.hitl.FormResponse;
import com.embabel.gekko.domain.ResearchTypes;
import com.embabel.ux.form.Form;
import com.embabel.ux.form.FormSubmission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST API for the trading research workflow.
 * Mirrors the HTMX controller flow but returns JSON responses.
 */
@RestController
@RequestMapping("/api")
public class TradingApiController {

    private static final Logger logger = LoggerFactory.getLogger(TradingApiController.class);

    private final AgentPlatform agentPlatform;

    public TradingApiController(AgentPlatform agentPlatform) {
        this.agentPlatform = agentPlatform;
    }

    /* =======================
       POST /api/plan — Start research
       ======================= */

    @PostMapping("/plan")
    public ResponseEntity<Map<String, Object>> planResearch(@RequestBody TickerRequest request) {
        var ticker = new ResearchTypes.Ticker(request.ticker(), request.feedback() != null ? request.feedback() : "");

        var agent = agentPlatform.agents()
                .stream()
                .filter(a -> a.getName().toLowerCase().contains("orchestrator"))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException("No orchestrator agent found. Please ensure the orchestrator agent is registered.")
                );

        var agentProcess = agentPlatform.createAgentProcessFrom(
                agent,
                ProcessOptions.DEFAULT
                        .withVerbosity(new Verbosity(true, true))
                        .withBudget(new Budget().withTokens(16384)),
                ticker
        );

        agentPlatform.start(agentProcess);

        // Check if process is already WAITING (plan generated, awaiting approval)
        AgentProcess process = agentPlatform.getAgentProcess(agentProcess.getId());

        if (process != null && process.getStatus() == AgentProcessStatusCode.WAITING) {
            String planContent = extractPlanContent(process);
            return ResponseEntity.ok(Map.of(
                    "processId", agentProcess.getId(),
                    "status", "WAITING",
                    "plan", planContent != null ? planContent : "",
                    "message", "Research plan generated. Please review and approve."
            ));
        }

        // Still running — return process ID for polling
        return ResponseEntity.ok(Map.of(
                "processId", agentProcess.getId(),
                "status", "RUNNING",
                "message", "Research in progress. Poll /api/plan/{processId}/status for updates."
        ));
    }

    /* =======================
       GET /api/plan/{processId}/status — Poll process status
       ======================= */

    @GetMapping("/plan/{processId}/status")
    public ResponseEntity<Map<String, Object>> getPlanStatus(@PathVariable String processId) {
        AgentProcess process = agentPlatform.getAgentProcess(processId);
        if (process == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Process not found: " + processId));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("processId", processId);
        response.put("status", process.getStatus().name());

        if (process.getStatus() == AgentProcessStatusCode.WAITING) {
            String planContent = extractPlanContent(process);
            response.put("plan", planContent != null ? planContent : "");
            response.put("message", "Plan generated. Submit approval to continue.");
        } else if (process.getStatus() == AgentProcessStatusCode.COMPLETED) {
            String investmentPlan = extractInvestmentPlan(process);
            response.put("investmentPlan", investmentPlan != null ? investmentPlan : "");
            response.put("message", "Research complete.");
        } else {
            response.put("message", "Process is " + process.getStatus().name().toLowerCase() + ".");
        }

        return ResponseEntity.ok(response);
    }

    /* =======================
       POST /api/plan/{processId}/approve — Approve plan
       ======================= */

    @PostMapping("/plan/{processId}/approve")
    public ResponseEntity<Map<String, Object>> approvePlan(
            @PathVariable String processId,
            @RequestBody ApprovalRequest request
    ) {
        AgentProcess process = agentPlatform.getAgentProcess(processId);
        if (process == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Process not found: " + processId));
        }

        synchronized (process) {
            if (process.getStatus() != AgentProcessStatusCode.WAITING) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Process is no longer in WAITING state. Current: " + process.getStatus()));
            }

            // Find the WaitFor form on the blackboard
            @SuppressWarnings("unchecked")
            List<FormBindingRequest<?>> requests = (List) process.getBlackboard().getObjects()
                    .stream()
                    .filter(FormBindingRequest.class::isInstance)
                    .map(o -> (FormBindingRequest<?>) o)
                    .toList();

            if (requests.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "No WaitFor form found for this process."));
            }

            FormBindingRequest<?> formBindingRequest = requests.get(0);
            Form form = (Form) formBindingRequest.getPayload();

            Map<String, Object> values = Map.of(
                    "approved", request.approved(),
                    "feedback", request.feedback() != null ? request.feedback() : ""
            );

            String submissionId = UUID.randomUUID().toString();
            FormSubmission submission = new FormSubmission(
                    form.getId().toString(), values, submissionId, java.time.Instant.now()
            );

            FormResponse response = new FormResponse(
                    UUID.randomUUID().toString(),
                    formBindingRequest.getId().toString(),
                    submission,
                    false,
                    java.time.Instant.now()
            );

            formBindingRequest.onResponse(response, process);

            try {
                agentPlatform.start(process);
            } catch (Exception e) {
                logger.error("Failed to resume process {} after WaitFor submission", processId, e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Failed to resume process: " + e.getMessage()));
            }
        }

        return ResponseEntity.ok(Map.of(
                "processId", processId,
                "status", "RESUMED",
                "message", "Plan approved. Research in progress. Poll /api/plan/" + processId + "/status for updates."
        ));
    }

    /* =======================
       Helpers
       ======================= */

    private String extractPlanContent(AgentProcess process) {
        try {
            var blackboard = process.getBlackboard();
            if (blackboard == null) return null;
            List<ResearchTypes.ResearchPlan> plans = blackboard.objectsOfType(ResearchTypes.ResearchPlan.class);
            if (!plans.isEmpty()) {
                return plans.get(0).content();
            }
        } catch (Exception e) {
            logger.warn("Failed to extract plan content: {}", e.getMessage());
        }
        return null;
    }

    private String extractInvestmentPlan(AgentProcess process) {
        try {
            var blackboard = process.getBlackboard();
            if (blackboard == null) return null;
            List<ResearchTypes.InvestmentPlan> plans = blackboard.objectsOfType(ResearchTypes.InvestmentPlan.class);
            if (!plans.isEmpty()) {
                return plans.get(0).content();
            }
        } catch (Exception e) {
            logger.warn("Failed to extract investment plan: {}", e.getMessage());
        }
        return null;
    }

    /* =======================
       Request DTOs
       ======================= */

    public static record TickerRequest(String ticker, String feedback) {}

    public static record ApprovalRequest(boolean approved, String feedback) {}
}
