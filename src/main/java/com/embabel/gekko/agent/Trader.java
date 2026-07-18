package com.embabel.gekko.agent;

import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.common.ActionContext;
import com.embabel.gekko.domain.ResearchTypes;
import com.embabel.gekko.domain.TraderProposalOutput;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.embabel.common.ai.model.ModelProvider.BEST_ROLE;

/**
 * Trader agent — translates the Research Manager's investment plan into a concrete
 * transaction proposal (Buy/Hold/Sell with entry price, stop-loss, position sizing).
 * Called directly by DebateAgent via createObject(), not as a sub-process agent.
 */
@Component
@RegisterReflectionForBinding(TraderProposalOutput.class)
@RequiredArgsConstructor
@Slf4j
public class Trader {

    @Action(description = "Produce concrete transaction proposal from research plan")
    public String traderProposal(
            ResearchTypes.Ticker ticker,
            String researchPlan,
            ActionContext actionContext
    ) {
        var model = Map.<String, Object>ofEntries(
                Map.entry("ticker", ticker.content()),
                Map.entry("research_plan", researchPlan)
        );

        try {
            var output = actionContext.ai()
                    .withLlmByRole(BEST_ROLE)
                    .withId("trader")
                    .withTemplate("managers/Trader")
                    .createObject(TraderProposalOutput.class, model);
            return output.render();
        } catch (Exception e) {
            log.warn("Structured trader proposal failed, falling back to free-text: {}", e.getMessage());
            return actionContext.ai()
                    .withLlmByRole(BEST_ROLE)
                    .withId("trader")
                    .withTemplate("managers/Trader")
                    .createObject(String.class, model);
        }
    }
}
