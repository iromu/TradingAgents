package com.embabel.gekko.web;

import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.Budget;
import com.embabel.agent.core.ProcessOptions;
import com.embabel.agent.core.Verbosity;
import com.embabel.gekko.htmx.GenericProcessingValues;
import lombok.Getter;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping({"/", "/travel/journey"})
public class TradingHtmxController {

    private final AgentPlatform agentPlatform;

    public TradingHtmxController(AgentPlatform agentPlatform) {
        this.agentPlatform = agentPlatform;
    }

    /* =======================
       Form DTOs
       ======================= */

    @Getter
    public static class TickerForm {
        private final String content = "NVDA";

    }

    @SuppressWarnings("SameReturnValue")
    @GetMapping
    public String showPlanForm(Model model) {
        model.addAttribute("ticker", new TickerForm());
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
                        .withBudget(new Budget(Budget.DEFAULT_TOKEN_LIMIT * 3)),
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
        return "common/processing";
    }
}
