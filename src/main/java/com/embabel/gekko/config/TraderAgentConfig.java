package com.embabel.gekko.config;

import com.embabel.agent.prompt.persona.RoleGoalBackstory;
import com.embabel.common.ai.model.LlmOptions;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.llm-options")
@Slf4j
public record TraderAgentConfig(
        LlmOptions tickerLlm,
        LlmOptions writerLlm,
        int maxConcurrency,
        RoleGoalBackstory researcher,
        RoleGoalBackstory outliner,
        RoleGoalBackstory writer,
        String outputDirectory,
        @Min(0) @Max(1) double similarityThreshold,
        @Min(1) int maxDebateIterations,
        @Min(1) int researchTokenBudget,
        @Min(1) int maxRiskDebateRounds,
        String provider,
        String bestModel,
        String cheapestModel,
        @Valid AnthropicProviderConfig anthropic,
        @Valid GoogleProviderConfig google,
        @Valid OpenAiProviderConfig openai
) {
    public record AnthropicProviderConfig(String effort) {}
    public record GoogleProviderConfig(String thinkingLevel) {}
    public record OpenAiProviderConfig(String reasoningEffort) {}

    public TraderAgentConfig {
        if (tickerLlm == null) {
            tickerLlm = LlmOptions.withDefaultLlm();
            log.info("Using default LLM options for tickerLlm");
        }
        if (writerLlm == null) {
            writerLlm = LlmOptions.withDefaultLlm();
            log.info("Using default LLM options for writerLlm");
        }
        if (similarityThreshold <= 0 || similarityThreshold > 1) {
            similarityThreshold = 0.8;
            log.info("Using default similarityThreshold: 0.8");
        }
        if (maxDebateIterations <= 0) {
            maxDebateIterations = 5;
            log.info("Using default maxDebateIterations: 5");
        }
        if (researchTokenBudget <= 0) {
            researchTokenBudget = 16384;
            log.info("Using default researchTokenBudget: 16384");
        }
        if (maxRiskDebateRounds <= 0) {
            maxRiskDebateRounds = 3;
            log.info("Using default maxRiskDebateRounds: 3");
        }
        if (bestModel != null && cheapestModel != null && bestModel.equals(cheapestModel)) {
            log.warn("Model roles 'best' and 'cheapest' both resolve to '{}'. " +
                    "Consider configuring distinct models via app.llm-options.best-model / cheapest-model.", bestModel);
        }
    }
}
