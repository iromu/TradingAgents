package com.embabel.gekko.web;

import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.AgentProcess;
import com.embabel.agent.core.AgentProcessStatusCode;
import com.embabel.gekko.domain.ResearchTypes;
import com.embabel.gekko.util.AgentUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * REST API for the trading research workflow.
 * Mirrors the HTMX controller flow but returns JSON responses.
 */
@RestController
@RequestMapping("/api")
public class TradingApiController {

    private static final Logger logger = LoggerFactory.getLogger(TradingApiController.class);

    private final AgentPlatform agentPlatform;
    private final ResearchPlanService researchPlanService;

    public TradingApiController(AgentPlatform agentPlatform, ResearchPlanService researchPlanService) {
        this.agentPlatform = agentPlatform;
        this.researchPlanService = researchPlanService;
    }

    @PostMapping("/plan")
    public ResponseEntity<Map<String, Object>> planResearch(@RequestBody TickerRequest request) {
        var ticker = new ResearchTypes.Ticker(request.ticker(), request.feedback() != null ? request.feedback() : "");
        var agentProcess = researchPlanService.createAndStart(ticker);
        var process = agentPlatform.getAgentProcess(agentProcess.getId());

        if (process != null && process.getStatus() == AgentProcessStatusCode.WAITING) {
            return ResponseEntity.ok(Map.of(
                    "processId", agentProcess.getId(),
                    "status", "WAITING",
                    "plan", AgentUtils.extractPlanContent(process),
                    "message", "Research plan generated. Please review and approve."
            ));
        }

        return ResponseEntity.ok(Map.of(
                "processId", agentProcess.getId(),
                "status", "RUNNING",
                "message", "Research in progress. Poll /api/plan/{processId}/status for updates."
        ));
    }

    @GetMapping("/plan/{processId}/status")
    public ResponseEntity<Map<String, Object>> getPlanStatus(@PathVariable String processId) {
        AgentUtils.validateProcessId(processId);
        var process = agentPlatform.getAgentProcess(processId);
        if (process == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Process not found: " + processId));
        }

        var response = new HashMap<String, Object>();
        response.put("processId", processId);
        response.put("status", process.getStatus().name());
        response.put("message", statusMessage(process));

        if (process.getStatus() == AgentProcessStatusCode.WAITING) {
            var waitingMap = new HashMap<String, Object>(response);
            waitingMap.put("plan", AgentUtils.extractPlanContent(process));
            waitingMap.put("message", "Plan generated. Submit approval to continue.");
            return ResponseEntity.ok(waitingMap);
        }
        if (process.getStatus() == AgentProcessStatusCode.COMPLETED) {
            var completedMap = new HashMap<String, Object>(response);
            completedMap.put("investmentPlan", AgentUtils.extractInvestmentPlan(process));
            completedMap.put("message", "Research complete.");
            return ResponseEntity.ok(completedMap);
        }
        return ResponseEntity.ok(response);
    }

    /**
     * Lightweight status endpoint for polling from the processing page.
     * Returns just the process status as JSON for SSE polling.
     */
    @GetMapping("/v1/process/{processId}/status")
    public ResponseEntity<Map<String, String>> getProcessStatus(@PathVariable String processId) {
        AgentUtils.validateProcessId(processId);
        var process = agentPlatform.getAgentProcess(processId);
        if (process == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Process not found"));
        }
        return ResponseEntity.ok(Map.of("status", process.getStatus().name()));
    }

    @PostMapping("/plan/{processId}/approve")
    public ResponseEntity<Map<String, Object>> approvePlan(
            @PathVariable String processId,
            @RequestBody ApprovalRequest request
    ) {
        AgentUtils.validateProcessId(processId);
        var process = agentPlatform.getAgentProcess(processId);
        if (process == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Process not found: " + processId));
        }

        synchronized (AgentUtils.getProcessLock(processId)) {
            if (process.getStatus() != AgentProcessStatusCode.WAITING) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Process is no longer in WAITING state. Current: " + process.getStatus()));
            }

            Map<String, Object> values = Map.of("approved", request.approved(), "feedback", request.feedback() != null ? request.feedback() : "");

            var resumed = researchPlanService.submitWaitForForm(process, values, "Failed to resume process");
            if (resumed == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Failed to resume process: " + processId));
            }
        }

        return ResponseEntity.ok(Map.of(
                "processId", processId,
                "status", "RESUMED",
                "message", "Plan approved. Research in progress. Poll /api/plan/" + processId + "/status for updates."
        ));
    }

    private String statusMessage(AgentProcess process) {
        return switch (process.getStatus()) {
            case WAITING -> "Plan generated. Submit approval to continue.";
            case COMPLETED -> "Research complete.";
            default -> "Process is " + process.getStatus().name().toLowerCase() + ".";
        };
    }

    public static record TickerRequest(String ticker, String feedback) {}

    public static record ApprovalRequest(boolean approved, String feedback) {}
}