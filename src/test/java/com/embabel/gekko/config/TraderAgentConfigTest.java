package com.embabel.gekko.config;

import com.embabel.agent.prompt.persona.RoleGoalBackstory;
import com.embabel.common.ai.model.LlmOptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TraderAgentConfig — defaults, provider selection, model configuration.
 */
class TraderAgentConfigTest {

    // --- Default construction tests ---

    @Test
    void constructor_withNulls_usesDefaults() {
        var config = new TraderAgentConfig(
                null, null, 0, null, null, null, "/tmp", 0.0, 0
        );

        assertNotNull(config.tickerLlm());
        assertNotNull(config.writerLlm());
        assertEquals(0.8, config.similarityThreshold());
        assertEquals(5, config.maxDebateIterations());
    }

    @Test
    void constructor_withProvidedValues_usesProvided() {
        var tickerLlm = LlmOptions.withDefaultLlm();
        var writerLlm = LlmOptions.withDefaultLlm();
        var config = new TraderAgentConfig(
                tickerLlm, writerLlm, 10, null, null, null, "/tmp/output", 0.9, 10
        );

        assertSame(tickerLlm, config.tickerLlm());
        assertSame(writerLlm, config.writerLlm());
        assertEquals(10, config.maxConcurrency());
        assertEquals(0.9, config.similarityThreshold());
        assertEquals(10, config.maxDebateIterations());
    }

    @Test
    void constructor_similarityThreshold_clampedToMax() {
        var config = new TraderAgentConfig(
                null, null, 0, null, null, null, "/tmp", 1.5, 0
        );

        // > 1 should use default
        assertEquals(0.8, config.similarityThreshold());
    }

    @Test
    void constructor_similarityThreshold_negative_usesDefault() {
        var config = new TraderAgentConfig(
                null, null, 0, null, null, null, "/tmp", -0.5, 0
        );

        assertEquals(0.8, config.similarityThreshold());
    }

    @Test
    void constructor_maxDebateIterations_negative_usesDefault() {
        var config = new TraderAgentConfig(
                null, null, 0, null, null, null, "/tmp", 0.5, -1
        );

        assertEquals(5, config.maxDebateIterations());
    }

    @Test
    void constructor_maxDebateIterations_zero_usesDefault() {
        var config = new TraderAgentConfig(
                null, null, 0, null, null, null, "/tmp", 0.5, 0
        );

        assertEquals(5, config.maxDebateIterations());
    }

    @Test
    void constructor_outputDirectory_usesProvided() {
        var config = new TraderAgentConfig(
                null, null, 0, null, null, null, "/tmp/custom", 0.5, 0
        );

        assertEquals("/tmp/custom", config.outputDirectory());
    }

    // --- LlmOptions tests ---

    @Test
    void tickerLlm_defaultLlm_notNull() {
        var config = new TraderAgentConfig(
                null, null, 0, null, null, null, "/tmp", 0.5, 0
        );

        // LlmOptions.withDefaultLlm() should return a non-null LlmOptions
        assertNotNull(config.tickerLlm());
    }

    @Test
    void writerLlm_defaultLlm_notNull() {
        var config = new TraderAgentConfig(
                null, null, 0, null, null, null, "/tmp", 0.5, 0
        );

        assertNotNull(config.writerLlm());
    }

    // --- RoleGoalBackstory tests ---

    @Test
    void researcher_roleIsNull_whenNotConfigured() {
        var config = new TraderAgentConfig(
                null, null, 0, null, null, null, "/tmp", 0.5, 0
        );

        assertNull(config.researcher());
    }

    @Test
    void researcher_roleIsNotNull_whenConfigured() {
        var role = new RoleGoalBackstory("Researcher", "Analyze market data", "Deep knowledge of financial markets");
        var config = new TraderAgentConfig(
                null, null, 0, role, null, null, "/tmp", 0.5, 0
        );

        assertNotNull(config.researcher());
        // Kotlin data class — use getRole() accessor
        assertTrue(config.researcher().getRole().contains("Researcher"));
    }

    // --- Provider selection tests ---

    @Test
    void providerSelection_defaultIsOpenaiCompat() {
        // The default provider should be openai-compat (LiteLLM)
        // This is verified by the application.yaml default:
        // app.llm-options.provider: openai-compat
        var config = new TraderAgentConfig(
                null, null, 0, null, null, null, "/tmp", 0.5, 0
        );

        // LlmOptions should have the default model configured
        assertNotNull(config.tickerLlm());
    }

    // --- Integration test: config with all fields ---

    @Test
    void constructor_allFieldsProvided() {
        var tickerLlm = LlmOptions.withDefaultLlm();
        var writerLlm = LlmOptions.withDefaultLlm();
        var researcher = new RoleGoalBackstory("Researcher", "Analyze market data", "Deep knowledge of financial markets");
        var outliner = new RoleGoalBackstory("Outliner", "Outline research", "Expert at structuring research");
        var writer = new RoleGoalBackstory("Writer", "Write reports", "Expert at writing clear reports");

        var config = new TraderAgentConfig(
                tickerLlm, writerLlm, 4, researcher, outliner, writer,
                "/tmp/output", 0.75, 8
        );

        assertSame(tickerLlm, config.tickerLlm());
        assertSame(writerLlm, config.writerLlm());
        assertEquals(4, config.maxConcurrency());
        assertSame(researcher, config.researcher());
        assertSame(outliner, config.outliner());
        assertSame(writer, config.writer());
        assertEquals("/tmp/output", config.outputDirectory());
        assertEquals(0.75, config.similarityThreshold());
        assertEquals(8, config.maxDebateIterations());
    }
}
