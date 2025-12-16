package com.embabel.gekko.htmx;

import com.embabel.agent.core.AgentProcess;
import org.springframework.ui.Model;

/**
 * Generic processing values to be used in the model for HTMX responses
 * across different apps.
 * This allows for consistent handling of agent processes and page details.
 */
public record GenericProcessingValues(AgentProcess agentProcess, String pageTitle, String detail, String resultModelKey,
                                      String successView) {

    public void addToModel(Model model) {
        model.addAttribute("processId", agentProcess.getId());
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("detail", detail);
        model.addAttribute("resultModelKey", resultModelKey);
        model.addAttribute("successView", successView);
    }
}
