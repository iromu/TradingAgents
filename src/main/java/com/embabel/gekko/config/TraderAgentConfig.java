package com.embabel.gekko.config;

import com.embabel.agent.prompt.persona.RoleGoalBackstory;
import com.embabel.common.ai.model.LlmOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.llm-options")
public record TraderAgentConfig(
        LlmOptions tickerLlm,
        LlmOptions writerLlm,
        int maxConcurrency,
        RoleGoalBackstory researcher,
        RoleGoalBackstory outliner,
        RoleGoalBackstory writer,
        String outputDirectory,
        double similarityThreshold,
        int maxDebateIterations
) {
    private static final Logger log = LoggerFactory.getLogger(TraderAgentConfig.class);
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
