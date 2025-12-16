package com.embabel.gekko.htmx;


import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.AgentProcessStatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class ProcessStatusController {

    private static final Logger logger =
            LoggerFactory.getLogger(ProcessStatusController.class);

    private final AgentPlatform agentPlatform;

    public ProcessStatusController(AgentPlatform agentPlatform) {
        this.agentPlatform = agentPlatform;
    }

    /**
     * The HTML page that shows the status of the plan generation.
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
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Process not found"
            );
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
                logger.error(
                        "Process {} failed: {}",
                        processId,
                        agentProcess.getFailureInfo()
                );
                model.addAttribute(
                        "error",
                        "Failed to generate plan: " + agentProcess.getFailureInfo()
                );
                return "common/processing-error";
            }

            case TERMINATED -> {
                logger.info("Process {} was terminated", processId);
                model.addAttribute(
                        "error",
                        "Process was terminated before completion"
                );
                return "common/processing-error";
            }

            default -> {
                model.addAttribute("processId", processId);
                model.addAttribute("pageTitle", "Planning...");
                // Keep showing loading state
                return "common/processing";
            }
        }
    }
}
