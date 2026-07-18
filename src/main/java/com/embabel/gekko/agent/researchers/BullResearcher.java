package com.embabel.gekko.agent.researchers;

import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.common.ActionContext;
import com.embabel.gekko.domain.ResearchTypes;
import com.embabel.gekko.util.AgentUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.embabel.common.ai.model.ModelProvider.BEST_ROLE;

/**
 * Bull researcher — generates a bullish argument based on analyst briefs and debate history.
 * Called directly by DebateLoopAgent via createObject(), not as a sub-process agent.
 */
@Component
@RequiredArgsConstructor
public class BullResearcher {

    @Action(description = "Generate a bullish argument from analyst briefs and debate history")
    public String argue(
            ResearchTypes.DebateBriefs briefs,
            List<String> history,
            ActionContext actionContext
    ) {
        String previousResponse = history.isEmpty() ? "No argument yet." : history.getLast();

        return "# Bull Analyst\n" + actionContext.ai()
                .withLlmByRole(BEST_ROLE)
                .withId("bullResearcher")
                .withTemplate("researchers/BullResearcher")
                .createObject(String.class, Map.of(
                        "fundamentalsBrief", briefs.fundamentalsBrief(),
                        "marketBrief", briefs.marketBrief(),
                        "newsBrief", briefs.newsBrief(),
                        "socialBrief", briefs.socialBrief(),
                        "history", history.isEmpty() ? "No history yet." : String.join("\n", history),
                        "current_response", previousResponse,
                        "past_memory_str", AgentUtils.NO_PAST_MEMORY
                ));
    }
}
