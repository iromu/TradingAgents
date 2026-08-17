package com.embabel.gekko.agent.identity;

import com.embabel.common.ai.prompt.PromptContributor;
import com.embabel.gekko.util.PromptSanitizer;
import org.springframework.stereotype.Component;

/**
 * Injects resolved instrument identity into LLM prompts to prevent hallucination.
 * Fail-open: if no InstrumentContext is available, contributes nothing.
 *
 * Thread-safe: uses ThreadLocal to prevent cross-contamination between concurrent
 * agent processes running on different threads. Context is set and cleared per
 * agent turn by OrchestratorAgent to avoid stale data leaking across turns.
 */
@Component
public class InstrumentContextPromptContributor implements PromptContributor {

    /** Per-thread context to prevent cross-request contamination. */
    private final ThreadLocal<InstrumentContext> context = new ThreadLocal<>();

    /**
     * Set by OrchestratorAgent after resolving identity.
     * Clears any previous context for this thread first.
     */
    public void setContext(InstrumentContext ctx) {
        if (ctx == null) {
            context.remove();
            return;
        }
        context.set(ctx);
    }

    /**
     * Clear the ThreadLocal after an agent turn completes.
     * Call from OrchestratorAgent after the full turn to prevent stale context
     * leaking if the thread is reused for a different ticker.
     */
    public void clear() {
        context.remove();
    }

    @Override
    public String contribution() {
        InstrumentContext ctx = context.get();
        if (ctx == null) {
            return "";
        }
        String name = PromptSanitizer.sanitizeForPrompt(ctx.companyName());
        String ticker = PromptSanitizer.sanitizeForPrompt(ctx.ticker());
        String sector = PromptSanitizer.sanitizeForPrompt(ctx.sector());
        String industry = PromptSanitizer.sanitizeForPrompt(ctx.industry());
        String exchange = PromptSanitizer.sanitizeForPrompt(ctx.exchange());
        return """
                INSTRUMENT CONTEXT:
                You are analyzing: %s (%s)
                Sector: %s
                Industry: %s
                Exchange: %s

                IMPORTANT: You are analyzing %s. Do not confuse it with any other company.
                All price data, news, and analysis MUST refer to %s.
                """.formatted(name, ticker, sector, industry, exchange, name, name);
    }
}
