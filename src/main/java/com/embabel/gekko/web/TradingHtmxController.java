package com.embabel.gekko.web;

import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.AgentProcess;
import com.embabel.agent.core.Budget;
import com.embabel.agent.core.ProcessOptions;
import com.embabel.agent.core.AgentProcessStatusCode;
import com.embabel.agent.core.Verbosity;
import com.embabel.agent.core.hitl.Awaitable;
import com.embabel.agent.core.hitl.FormBindingRequest;
import com.embabel.agent.core.hitl.FormResponse;
import com.embabel.gekko.agent.TraderAgent.ResearchPlan;
import com.embabel.gekko.htmx.GenericProcessingValues;
import com.embabel.ux.form.Form;
import com.embabel.ux.form.FormSubmission;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping({"/", "/travel/journey"})
public class TradingHtmxController {

    private static final Logger logger = LoggerFactory.getLogger(TradingHtmxController.class);

    private final AgentPlatform agentPlatform;

    public TradingHtmxController(AgentPlatform agentPlatform) {
        this.agentPlatform = agentPlatform;
    }

    /* =======================
       Form DTOs
       ======================= */

    @Data
    public static class TickerForm {
        private final String content;
        private final String feedback;
    }

    @SuppressWarnings("SameReturnValue")
    @GetMapping
    public String showPlanForm(Model model) {
        model.addAttribute("ticker", new TickerForm("NVDA", ""));
        return "form";
    }

    @SuppressWarnings("SameReturnValue")
    @PostMapping("/plan")
    public String planJourney(
            @ModelAttribute TickerForm form,
            Model model
    ) {

        var agent = agentPlatform.agents()
                .stream()
                .filter(a -> a.getName().toLowerCase().contains("trader"))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException(
                                "No trading agent found. Please ensure the tripper agent is registered."
                        )
                );

        var agentProcess = agentPlatform.createAgentProcessFrom(
                agent,
                ProcessOptions.DEFAULT
                        .withVerbosity(new Verbosity(true, true))
//                        .withBudget(new Budget(Budget.DEFAULT_TOKEN_LIMIT * 3)),
                        .withBudget(new Budget().withTokens(16384)),
                form
        );

        model.addAttribute("ticker", form);

        new GenericProcessingValues(
                agentProcess,
                "Planning your journey",
                form.content,
                "travelPlan",
                "plan"
        ).addToModel(model);

        agentPlatform.start(agentProcess);

        // Check if the process has already entered WAITING state (plan generated, awaiting approval)
        // The process may have transitioned quickly through RUNNING → WAITING
        try {
            Thread.sleep(500);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }

        AgentProcess process = agentPlatform.getAgentProcess(agentProcess.getId());
        if (process != null && process.getStatus() == AgentProcessStatusCode.WAITING) {
            // Redirect to plan review page
            return "redirect:/plan/review/" + agentProcess.getId();
        }

        // Still running — show processing page with SSE updates
        model.addAttribute("ticker", form);

        new GenericProcessingValues(
                agentProcess,
                "Planning your journey",
                form.content,
                "travelPlan",
                "plan"
        ).addToModel(model);

        return "common/processing";
    }

    /**
     * Display the plan review page when the agent process is in WAITING state.
     * The plan has been generated and is awaiting user approval.
     */
    @GetMapping("/plan/review/{processId}")
    public String reviewPlan(
            @PathVariable String processId,
            Model model,
            RedirectAttributes redirectAttrs
    ) {
        AgentProcess process = agentPlatform.getAgentProcess(processId);
        if (process == null) {
            redirectAttrs.addFlashAttribute("error", "Process not found: " + processId);
            return "redirect:/";
        }

        if (process.getStatus() != AgentProcessStatusCode.WAITING) {
            // Process hasn't reached WAITING yet — redirect back to processing
            return "redirect:/plan/status/" + processId;
        }

        // Extract the plan content from the blackboard FormBindingRequest
        String planContent = extractPlanContent(process);
        if (planContent == null || planContent.isEmpty()) {
            redirectAttrs.addFlashAttribute("error", "Plan not yet available. Please wait.");
            return "redirect:/plan/status/" + processId;
        }

        model.addAttribute("processId", processId);
        model.addAttribute("planContent", planContent);
        model.addAttribute("pageTitle", "Review Research Plan");
        model.addAttribute("detail", "Research plan generated for: " + processId);

        return "plan-review";
    }

    /**
     * Submit the plan approval form.
     * Resubmits the form data back to the agent process, which resumes execution.
     */
    @PostMapping("/plan/review/{processId}")
    public String submitPlanApproval(
            @PathVariable String processId,
            @RequestParam String approved,
            @RequestParam(required = false) String feedback,
            RedirectAttributes redirectAttrs
    ) {
        AgentProcess process = agentPlatform.getAgentProcess(processId);
        if (process == null) {
            redirectAttrs.addFlashAttribute("error", "Process not found: " + processId);
            return "redirect:/";
        }

        synchronized (process) {
            // Check process is still WAITING — prevent duplicate submissions
            if (process.getStatus() != AgentProcessStatusCode.WAITING) {
                redirectAttrs.addFlashAttribute("error", "Process is no longer in WAITING state.");
                return "redirect:/plan/review/" + processId;
            }

            // Find the FormBindingRequest on the blackboard
            @SuppressWarnings("unchecked")
            List<FormBindingRequest<?>> requests = (List) process.getBlackboard().getObjects()
                    .stream()
                    .filter(FormBindingRequest.class::isInstance)
                    .map(o -> (FormBindingRequest<?>) o)
                    .toList();

            if (requests.isEmpty()) {
                redirectAttrs.addFlashAttribute("error", "No WaitFor form found for this process.");
                return "redirect:/plan/review/" + processId;
            }

            FormBindingRequest<?> request = requests.get(0);
            Form form = (Form) request.getPayload();

            // Build the form submission values map
            // The form has fields: approved (boolean) and feedback (String)
            Map<String, Object> values = Map.of("approved", Boolean.parseBoolean(approved), "feedback", feedback != null ? feedback : "");

            String submissionId = UUID.randomUUID().toString();
            FormSubmission submission = new FormSubmission(form.getId().toString(), values, submissionId, java.time.Instant.now());

            // Create the form response
            FormResponse response = new FormResponse(
                    UUID.randomUUID().toString(),
                    request.getId().toString(),
                    submission,
                    false,
                    java.time.Instant.now()
            );

            // Process the form response — this binds the result to the blackboard
            request.onResponse(response, process);

            // Resume the process
            try {
                agentPlatform.start(process);
            } catch (Exception e) {
                logger.error("Failed to resume process {} after WaitFor submission", processId, e);
                redirectAttrs.addFlashAttribute("error", "Failed to resume process: " + e.getMessage());
                return "redirect:/plan/review/" + processId;
            }
        }

        redirectAttrs.addFlashAttribute("success", "Plan approved. Research in progress...");
        return "redirect:/plan/status/" + processId;
    }

    /**
     * Status page endpoint — shows the processing page for a given process.
     * Used as a redirect target after plan approval submission.
     */
    @GetMapping("/plan/status/{processId}")
    public String planStatus(
            @PathVariable String processId,
            Model model
    ) {
        AgentProcess process = agentPlatform.getAgentProcess(processId);
        if (process == null) {
            return "redirect:/";
        }

        model.addAttribute("processId", processId);

        if (process.getStatus() == AgentProcessStatusCode.WAITING) {
            // Process is waiting for plan approval — redirect to review page
            return "redirect:/plan/review/" + processId;
        }

        // Still running or completed — show processing page
        model.addAttribute("pageTitle", "Research in Progress");
        model.addAttribute("detail", "Research plan for: " + processId);
        model.addAttribute("resultModelKey", "researchResult");
        model.addAttribute("successView", "plan");

        new GenericProcessingValues(
                process,
                "Research in Progress",
                "Research plan for: " + processId,
                "researchResult",
                "plan"
        ).addToModel(model);

        return "common/processing";
    }

    /**
     * Extract the research plan content from the blackboard.
     * Uses the same pattern as ProcessStatusController.renderWaitingForm.
     */
    private String extractPlanContent(AgentProcess process) {
        try {
            var blackboard = process.getBlackboard();
            if (blackboard == null) {
                return null;
            }
            List<ResearchPlan> plans = blackboard.objectsOfType(ResearchPlan.class);
            if (!plans.isEmpty()) {
                return plans.get(0).content();
            }
            return null;
        } catch (Exception e) {
            logger.warn("Failed to extract plan content from blackboard: {}", e.getMessage());
            return null;
        }
    }
}
