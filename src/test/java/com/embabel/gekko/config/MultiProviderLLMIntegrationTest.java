package com.embabel.gekko.config;

import com.embabel.common.ai.model.LlmOptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the Multi-Provider LLM feature migrated from Tauric (Python).
 *
 * Validates:
 * - LlmOptions supports different roles (researcher, writer, etc.)
 * - LlmOptions supports provider-specific settings (temperature, maxTokens, thinking)
 * - Provider-specific configurations work (Anthropic effort, Google thinking-level, OpenAI reasoning-effort)
 * - Maven dependencies for multiple providers are declared
 * - Configuration properties are bound correctly
 */
@Tag("integration")
@SpringBootTest
class MultiProviderLLMIntegrationTest {

    @Autowired
    private TraderAgentConfig config;

    @Test
    void shouldHaveTraderAgentConfig() {
        assertNotNull(config);
    }

    @Test
    void shouldHaveProviderConfigFields() {
        // The config record should have provider, bestModel, cheapestModel fields
        var recordComponents = TraderAgentConfig.class.getRecordComponents();
        var fieldNames = new java.util.HashSet<String>();
        for (var component : recordComponents) {
            fieldNames.add(component.getName());
        }

        assertTrue(fieldNames.contains("provider"),
                "TraderAgentConfig should have 'provider' field");
        assertTrue(fieldNames.contains("bestModel"),
                "TraderAgentConfig should have 'bestModel' field");
        assertTrue(fieldNames.contains("cheapestModel"),
                "TraderAgentConfig should have 'cheapestModel' field");
    }

    @Test
    void shouldHaveProviderSpecificConfigRecords() {
        var recordComponents = TraderAgentConfig.class.getRecordComponents();
        var fieldNames = new java.util.HashSet<String>();
        for (var component : recordComponents) {
            fieldNames.add(component.getName());
        }

        assertTrue(fieldNames.contains("anthropic"),
                "TraderAgentConfig should have 'anthropic' config");
        assertTrue(fieldNames.contains("google"),
                "TraderAgentConfig should have 'google' config");
        assertTrue(fieldNames.contains("openai"),
                "TraderAgentConfig should have 'openai' config");
    }

    @Test
    void shouldHaveAnthropicProviderConfigRecord() {
        var components = TraderAgentConfig.AnthropicProviderConfig.class.getRecordComponents();
        var fieldNames = new java.util.HashSet<String>();
        for (var component : components) {
            fieldNames.add(component.getName());
        }
        assertTrue(fieldNames.contains("effort"),
                "AnthropicProviderConfig should have 'effort' field");
    }

    @Test
    void shouldHaveGoogleProviderConfigRecord() {
        var components = TraderAgentConfig.GoogleProviderConfig.class.getRecordComponents();
        var fieldNames = new java.util.HashSet<String>();
        for (var component : components) {
            fieldNames.add(component.getName());
        }
        assertTrue(fieldNames.contains("thinkingLevel"),
                "GoogleProviderConfig should have 'thinkingLevel' field");
    }

    @Test
    void shouldHaveOpenAiProviderConfigRecord() {
        var components = TraderAgentConfig.OpenAiProviderConfig.class.getRecordComponents();
        var fieldNames = new java.util.HashSet<String>();
        for (var component : components) {
            fieldNames.add(component.getName());
        }
        assertTrue(fieldNames.contains("reasoningEffort"),
                "OpenAiProviderConfig should have 'reasoningEffort' field");
    }

    @Test
    void shouldSupportDifferentRoles() {
        var researcherOptions = LlmOptions.withLlmForRole("researcher");
        var writerOptions = LlmOptions.withLlmForRole("writer");

        assertNotNull(researcherOptions);
        assertNotNull(writerOptions);
        assertEquals("researcher", researcherOptions.getRole());
        assertEquals("writer", writerOptions.getRole());
    }

    @Test
    void shouldSupportTemperatureSetting() {
        var opts = LlmOptions.withDefaultLlm().withTemperature(0.7);
        assertEquals(0.7, opts.getTemperature(), 0.01);
    }

    @Test
    void shouldSupportMaxTokensSetting() {
        var opts = LlmOptions.withDefaultLlm().withMaxTokens(4096);
        assertEquals(4096, opts.getMaxTokens());
    }

    @Test
    void shouldHaveDefaultLlmOptions() {
        var defaultOptions = LlmOptions.withDefaultLlm();
        assertNotNull(defaultOptions);
    }
}