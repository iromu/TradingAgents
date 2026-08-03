package com.embabel.gekko.config;

import com.embabel.agent.prompt.persona.RoleGoalBackstory;
import com.embabel.common.ai.model.LlmOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

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
        double similarityThreshold,
        int maxDebateIterations,
        String provider,
        String bestModel,
        String cheapestModel,
        AnthropicProviderConfig anthropic,
        GoogleProviderConfig google,
        OpenAiProviderConfig openai
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
    }
}
