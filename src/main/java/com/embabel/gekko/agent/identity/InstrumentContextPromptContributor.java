package com.embabel.gekko.agent.identity;

import com.embabel.common.ai.prompt.PromptContributor;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Injects resolved instrument identity into LLM prompts to prevent hallucination.
 * Fail-open: if no InstrumentContext is available, contributes nothing.
 *
 * Uses a ConcurrentHashMap keyed by ticker to prevent cross-contamination between
 * concurrent requests. The current ticker is tracked via ThreadLocal so contribution()
 * can look up the correct context without a parameter.
 *
 * The context is set by OrchestratorAgent.resolveIdentity() via setContext().
 */
@Component
public class InstrumentContextPromptContributor implements PromptContributor {

    /** Per-ticker contexts to prevent cross-request contamination. */
    private final Map<String, InstrumentContext> contexts = new ConcurrentHashMap<>();

    /** ThreadLocal tracking the current ticker for this request. */
    private final ThreadLocal<String> currentTicker = new ThreadLocal<>();

    /**
     * Set by OrchestratorAgent after resolving identity.
     * Also sets the ThreadLocal ticker so contribution() can look up the right context.
     */
    public void setContext(InstrumentContext context) {
        if (context == null) {
            contexts.clear();
            currentTicker.remove();
            return;
        }
        contexts.put(context.ticker(), context);
        currentTicker.set(context.ticker());
    }

    /**
     * Clean up ThreadLocal to prevent memory leaks.
     */
    @PreDestroy
    public void cleanup() {
        currentTicker.remove();
    }

    @Override
    public String contribution() {
        String ticker = currentTicker.get();
        if (ticker == null) {
            return "";
        }
        InstrumentContext ctx = contexts.get(ticker);
        if (ctx == null) {
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
                ctx.companyName(), ctx.ticker(),
                ctx.sector(), ctx.industry(), ctx.exchange(),
                ctx.companyName(), ctx.companyName()
        );
    }
}
