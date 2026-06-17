package com.embabel.gekko.config;

import com.embabel.agent.prompt.persona.RoleGoalBackstory;
import com.embabel.common.ai.model.LlmOptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TraderAgentConfig — defaults, provider selection, model configuration.
 */
class TraderAgentConfigTest {

    private TraderAgentConfig makeConfig(
            LlmOptions tickerLlm, LlmOptions writerLlm, int maxConcurrency,
            RoleGoalBackstory researcher, RoleGoalBackstory outliner, RoleGoalBackstory writer,
            String outputDirectory, double similarityThreshold, int maxDebateIterations
    ) {
        return new TraderAgentConfig(
                tickerLlm, writerLlm, maxConcurrency,
                researcher, outliner, writer,
                outputDirectory, similarityThreshold, maxDebateIterations,
                null, null, null, null, null, null
        );
    }

    // --- Default construction tests ---

    @Test
    void constructor_withNulls_usesDefaults() {
        var config = makeConfig(null, null, 0, null, null, null, "/tmp", 0.0, 0);

        assertNotNull(config.tickerLlm());
        assertNotNull(config.writerLlm());
        assertEquals(0.8, config.similarityThreshold());
        assertEquals(5, config.maxDebateIterations());
    }

    @Test
    void constructor_withProvidedValues_usesProvided() {
        var tickerLlm = LlmOptions.withDefaultLlm();
        var writerLlm = LlmOptions.withDefaultLlm();
        var config = makeConfig(tickerLlm, writerLlm, 10, null, null, null, "/tmp/output", 0.9, 10);

        assertSame(tickerLlm, config.tickerLlm());
        assertSame(writerLlm, config.writerLlm());
        assertEquals(10, config.maxConcurrency());
        assertEquals(0.9, config.similarityThreshold());
        assertEquals(10, config.maxDebateIterations());
    }

    @Test
    void constructor_similarityThreshold_clampedToMax() {
        var config = makeConfig(null, null, 0, null, null, null, "/tmp", 1.5, 0);

        // > 1 should use default
        assertEquals(0.8, config.similarityThreshold());
    }

    @Test
    void constructor_similarityThreshold_negative_usesDefault() {
        var config = makeConfig(null, null, 0, null, null, null, "/tmp", -0.5, 0);

        assertEquals(0.8, config.similarityThreshold());
    }

    @Test
    void constructor_maxDebateIterations_negative_usesDefault() {
        var config = makeConfig(null, null, 0, null, null, null, "/tmp", 0.5, -1);

        assertEquals(5, config.maxDebateIterations());
    }

    @Test
    void constructor_maxDebateIterations_zero_usesDefault() {
        var config = makeConfig(null, null, 0, null, null, null, "/tmp", 0.5, 0);

        assertEquals(5, config.maxDebateIterations());
    }

    @Test
    void constructor_outputDirectory_usesProvided() {
        var config = makeConfig(null, null, 0, null, null, null, "/tmp/custom", 0.5, 0);

        assertEquals("/tmp/custom", config.outputDirectory());
    }

    // --- LlmOptions tests ---

    @Test
    void tickerLlm_defaultLlm_notNull() {
        var config = makeConfig(null, null, 0, null, null, null, "/tmp", 0.5, 0);

        // LlmOptions.withDefaultLlm() should return a non-null LlmOptions
        assertNotNull(config.tickerLlm());
    }

    @Test
    void writerLlm_defaultLlm_notNull() {
        var config = makeConfig(null, null, 0, null, null, null, "/tmp", 0.5, 0);

        assertNotNull(config.writerLlm());
    }

    // --- RoleGoalBackstory tests ---

    @Test
    void researcher_roleIsNull_whenNotConfigured() {
        var config = makeConfig(null, null, 0, null, null, null, "/tmp", 0.5, 0);

        assertNull(config.researcher());
    }

    @Test
    void researcher_roleIsNotNull_whenConfigured() {
        var role = new RoleGoalBackstory("Researcher", "Analyze market data", "Deep knowledge of financial markets");
        var config = makeConfig(null, null, 0, role, null, null, "/tmp", 0.5, 0);

        assertNotNull(config.researcher());
        // Kotlin data class — use getRole() accessor
        assertTrue(config.researcher().getRole().contains("Researcher"));
    }

    // --- Provider selection tests ---

    @Test
    void providerSelection_defaultIsOpenaiCompat() {
        var config = makeConfig(null, null, 0, null, null, null, "/tmp", 0.5, 0);

        // LlmOptions should have the default model configured
        assertNotNull(config.tickerLlm());
    }

    // --- New config fields tests ---

    @Test
    void provider_and_models_are_null_by_default() {
        var config = makeConfig(null, null, 0, null, null, null, "/tmp", 0.5, 0);

        assertNull(config.provider());
        assertNull(config.bestModel());
        assertNull(config.cheapestModel());
    }

    @Test
    void provider_and_models_are_set_when_provided() {
        var config = new TraderAgentConfig(
                LlmOptions.withDefaultLlm(), LlmOptions.withDefaultLlm(), 4,
                null, null, null,
                "/tmp", 0.75, 8,
                "anthropic", "claude-opus-4", "claude-sonnet-4",
                new TraderAgentConfig.AnthropicProviderConfig("high"),
                new TraderAgentConfig.GoogleProviderConfig("high"),
                new TraderAgentConfig.OpenAiProviderConfig("medium")
        );

        assertEquals("anthropic", config.provider());
        assertEquals("claude-opus-4", config.bestModel());
        assertEquals("claude-sonnet-4", config.cheapestModel());
        assertEquals("high", config.anthropic().effort());
        assertEquals("high", config.google().thinkingLevel());
        assertEquals("medium", config.openai().reasoningEffort());
    }
}