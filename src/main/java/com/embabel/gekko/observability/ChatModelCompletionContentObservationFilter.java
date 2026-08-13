package com.embabel.gekko.observability;

import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationFilter;
import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.ai.content.Content;
import org.springframework.ai.observation.ObservabilityHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Observation filter that extracts prompt and completion text from a
 * {@link ChatModelObservationContext} and attaches them as high-cardinality
 * key-values for observability/tracing systems (Micrometer Observation).
 *
 * <p>This filter inspects the chat model observation context and pulls
 * - the list of prompt instructions (concatenated into {@code gen_ai.prompt})
 * - the model completions (concatenated into {@code gen_ai.completion})
 * <p>
 * These values are added as high-cardinality key-values which are useful for
 * debugging and tracing generation results. Content is truncated to
 * {@link #MAX_CONTENT_LENGTH} characters and ticker-like patterns are
 * redacted to {@code [TICKER]} to limit PII leakage.</p>
 */
@Component
public class ChatModelCompletionContentObservationFilter implements ObservationFilter {

    private static final int MAX_CONTENT_LENGTH = 500;

    private static final Pattern TICKER_PATTERN = Pattern.compile("\\b[A-Z]{1,6}\\b");

    /**
     * Map an incoming observation context to a possibly enriched context.
     * <p>
     * If the provided {@code context} is an instance of
     * {@link ChatModelObservationContext} this method will extract prompt and
     * completion text, concatenate them, and attach them as high-cardinality
     * key-values named {@code gen_ai.prompt} and {@code gen_ai.completion}.
     * Otherwise the original context is returned unchanged.
     *
     * @param context the incoming observation context
     * @return the (possibly) enriched context
     */
    @Override
    public Observation.Context map(Observation.Context context) {
        if (!(context instanceof ChatModelObservationContext chatModelObservationContext)) {
            return context; // Not a chat model context — nothing to do
        }

        // Extract prompt texts and completion texts as lists of strings
        var prompts = processPrompts(chatModelObservationContext);
        var completions = processCompletion(chatModelObservationContext);

        // Attach concatenated prompts as a high-cardinality key (redacted)
        chatModelObservationContext.addHighCardinalityKeyValue(
                RedactedKeyValue.of("gen_ai.prompt",
                        ObservabilityHelper.concatenateStrings(prompts)));

        // Attach concatenated completions as a high-cardinality key (redacted)
        chatModelObservationContext.addHighCardinalityKeyValue(
                RedactedKeyValue.of("gen_ai.completion",
                        ObservabilityHelper.concatenateStrings(completions)));

        // Agent Name (disabled)
        return chatModelObservationContext;
    }

    /**
     * Extracts the textual prompt instructions from the observation context.
     *
     * <p>This method will return an empty list if the request or its instructions
     * are null/empty. Otherwise it maps each {@link Content} instruction to its
     * text value.</p>
     *
     * @param chatModelObservationContext the chat model context containing the request
     * @return list of prompt texts, or an empty list if none available
     */
    private List<String> processPrompts(ChatModelObservationContext chatModelObservationContext) {
        // Guard against null request or empty instructions
        return CollectionUtils.isEmpty((chatModelObservationContext.getRequest()).getInstructions())
                ? List.of()
                : (chatModelObservationContext.getRequest()).getInstructions().stream()
                .map(Content::getText)
                .toList();
    }

    /**
     * Extracts the textual completions (model outputs) from the response.
     *
     * <p>Behavior:
     * - If the response or its results are null/empty an empty list is returned.
     * - Otherwise the method filters out any result entries without output text
     *   and returns a list of the remaining output texts.</p>
     *
     * @param context the chat model observation context containing the response
     * @return list of completion texts, or an empty list
     */
    private List<String> processCompletion(ChatModelObservationContext context) {
        if (context.getResponse() != null && (context.getResponse()).getResults() != null && !CollectionUtils.isEmpty((context.getResponse()).getResults())) {
            // If the consolidated result text is not present, return empty list
            return !StringUtils.hasText((context.getResponse()).getResult().getOutput().getText())
                    ? List.of()
                    : (context.getResponse()).getResults().stream()
                    .filter((generation) -> generation.getOutput() != null && StringUtils.hasText(generation.getOutput().getText()))
                    .map((generation) -> generation.getOutput().getText())
                    .toList();
        } else {
            return List.of();
        }
    }

    /**
     * Reusable {@link KeyValue} implementation that redacts ticker-like
     * patterns and truncates long values.
     * <p>
     * Replaces word tokens matching {@code [A-Z]{1,6}} with {@code [TICKER]}
     * and truncates the result to {@link #MAX_CONTENT_LENGTH} characters to
     * limit PII leakage and high-cardinality trace attribute bloat.</p>
     */
    private static final class RedactedKeyValue implements KeyValue {

        private final String key;
        private final String redactedValue;

        private RedactedKeyValue(String key, String redactedValue) {
            this.key = key;
            this.redactedValue = redactedValue;
        }

        static RedactedKeyValue of(String key, String raw) {
            if (raw == null) {
                return new RedactedKeyValue(key, null);
            }
            String redacted = TICKER_PATTERN.matcher(raw).replaceAll("[TICKER]");
            if (redacted.length() > MAX_CONTENT_LENGTH) {
                redacted = redacted.substring(0, MAX_CONTENT_LENGTH) + "… (truncated)";
            }
            return new RedactedKeyValue(key, redacted);
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public String getValue() {
            return redactedValue;
        }
    }
}
