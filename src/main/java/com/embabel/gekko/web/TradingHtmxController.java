package com.embabel.gekko.web;

import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.AgentProcess;
import com.embabel.agent.core.AgentProcessStatusCode;
import com.embabel.gekko.htmx.GenericProcessingValues;
import com.embabel.gekko.util.AgentUtils;
import com.embabel.gekko.web.ResearchPlanService;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping({"/", "/research"})
public class TradingHtmxController {

    private static final Logger logger = LoggerFactory.getLogger(TradingHtmxController.class);

    private final AgentPlatform agentPlatform;
    private final ResearchPlanService researchPlanService;

    public TradingHtmxController(AgentPlatform agentPlatform, ResearchPlanService researchPlanService) {
        this.agentPlatform = agentPlatform;
        this.researchPlanService = researchPlanService;
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
    public String planResearch(
            @ModelAttribute TickerForm form,
            Model model
    ) {
        var agentProcess = researchPlanService.createAndStart(form);

        model.addAttribute("ticker", form);

        new GenericProcessingValues(
                agentProcess,
                "Planning your research",
                form.content,
                "researchPlan",
                "plan"
        ).addToModel(model);

        // Check if the process has already entered WAITING state (plan generated, awaiting approval)
        AgentProcess process = agentPlatform.getAgentProcess(agentProcess.getId());
        if (process != null && process.getStatus() == AgentProcessStatusCode.WAITING) {
            return "redirect:/plan/review/" + agentProcess.getId();
        }

        // Still running — show processing page with SSE updates
        model.addAttribute("ticker", form);

        new GenericProcessingValues(
                agentProcess,
                "Planning your research",
                form.content,
                "researchPlan",
                "plan"
        ).addToModel(model);

        return "common/processing";
    }

    /**
     * Display the plan review page when the agent process is in WAITING state.
     */
    @GetMapping("/plan/review/{processId}")
    public String reviewPlan(
            @PathVariable String processId,
            Model model,
            RedirectAttributes redirectAttrs
    ) {
        AgentUtils.validateProcessId(processId);
        AgentProcess process = agentPlatform.getAgentProcess(processId);
        if (process == null) {
            redirectAttrs.addFlashAttribute("error", "Process not found: " + processId);
            return "redirect:/";
        }

        if (process.getStatus() != AgentProcessStatusCode.WAITING) {
            return "redirect:/plan/status/" + processId;
        }

        String planContent = AgentUtils.extractPlanContent(process);
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
     */
    @PostMapping("/plan/review/{processId}")
    public String submitPlanApproval(
            @PathVariable String processId,
            @RequestParam String approved,
            @RequestParam(required = false) String feedback,
            RedirectAttributes redirectAttrs
    ) {
        AgentUtils.validateProcessId(processId);
        AgentProcess process = agentPlatform.getAgentProcess(processId);
        if (process == null) {
            redirectAttrs.addFlashAttribute("error", "Process not found: " + processId);
            return "redirect:/";
        }

        synchronized (AgentUtils.getProcessLock(processId)) {
            if (process.getStatus() != AgentProcessStatusCode.WAITING) {
                redirectAttrs.addFlashAttribute("error", "Process is no longer in WAITING state.");
                return "redirect:/plan/review/" + processId;
            }

            Map<String, Object> values = Map.of("approved", "true".equalsIgnoreCase(approved), "feedback", feedback != null ? feedback : "");

            var resumed = researchPlanService.submitWaitForForm(process, values, "Failed to resume process");
            if (resumed == null) {
                redirectAttrs.addFlashAttribute("error", "Failed to resume process: " + processId);
                return "redirect:/plan/review/" + processId;
            }
        }

        redirectAttrs.addFlashAttribute("success", "Plan approved. Research in progress...");
        return "redirect:/plan/status/" + processId;
    }

    /**
     * Status page endpoint — shows the processing page for a given process.
     */
    @GetMapping("/plan/status/{processId}")
    public String planStatus(
            @PathVariable String processId,
            Model model
    ) {
        AgentUtils.validateProcessId(processId);
        AgentProcess process = agentPlatform.getAgentProcess(processId);
        if (process == null) {
            return "redirect:/";
        }

        model.addAttribute("processId", processId);

        if (process.getStatus() == AgentProcessStatusCode.WAITING) {
            return "redirect:/plan/review/" + processId;
        }

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
}