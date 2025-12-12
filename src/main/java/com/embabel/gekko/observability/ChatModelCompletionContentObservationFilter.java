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
 * debugging and tracing generation results. Be mindful of privacy and PII —
 * in production you may want to redact or sample these values.</p>
 */
@Component
public class ChatModelCompletionContentObservationFilter implements ObservationFilter {

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

        // Attach concatenated prompts as a high-cardinality key
        chatModelObservationContext.addHighCardinalityKeyValue(new KeyValue() {
            @Override
            public String getKey() {
                return "gen_ai.prompt";
            }

            @Override
            public String getValue() {
                // Use helper to safely concatenate list elements
                return ObservabilityHelper.concatenateStrings(prompts);
            }
        });

        // Attach concatenated completions as a high-cardinality key
        chatModelObservationContext.addHighCardinalityKeyValue(new KeyValue() {
            @Override
            public String getKey() {
                return "gen_ai.completion";
            }

            @Override
            public String getValue() {
                return ObservabilityHelper.concatenateStrings(completions);
            }
        });

        // Agent Name (disabled)
//        chatModelObservationContext.addHighCardinalityKeyValue(new KeyValue() {
//            @Override
//            public String getKey() {
//                return "gen_ai.agent_name";
//            }
//
//            @Override
//            public String getValue() {
//                return chatModelObservationContext.getRequest() != null
//                        ? chatModelObservationContext.getRequest().getInstructions().toString()
//                        : "unknown";
//            }
//        });
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
}
