package com.embabel.gekko.agent;

import com.embabel.agent.prompt.persona.RoleGoalBackstory;
import com.embabel.common.ai.model.LlmOptions;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.llm-options")
public record TraderAgentConfig(
        LlmOptions tickerLlm,
        LlmOptions writerLlm,
        int maxConcurrency,
        RoleGoalBackstory researcher,
        RoleGoalBackstory outliner,
        RoleGoalBackstory writer,
        String outputDirectory
) {
}
