package com.embabel.gekko.agent.identity;

import com.embabel.common.ai.prompt.PromptContributor;

/**
 * Injects resolved instrument identity into LLM prompts to prevent hallucination.
 * Fail-open: if no InstrumentContext is available, contributes nothing.
 */
public class InstrumentContextPromptContributor implements PromptContributor {

    private final InstrumentContext context;

    public InstrumentContextPromptContributor(InstrumentContext context) {
        this.context = context;
    }

    @Override
    public String contribution() {
        if (context == null) {
            return "";
        }
        return """
                INSTRUMENT CONTEXT:
                You are analyzing: %s (%s)
                Sector: %s
                Industry: %s
                Exchange: %s

                IMPORTANT: You are analyzing %s. Do not confuse it with any other company.
                All price data, news, and analysis MUST refer to %s.
                """.formatted(
                context.companyName(), context.ticker(),
                context.sector(), context.industry(), context.exchange(),
                context.companyName(), context.companyName()
        );
    }
}
