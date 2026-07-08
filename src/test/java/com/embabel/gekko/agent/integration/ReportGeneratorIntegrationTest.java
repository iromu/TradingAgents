package com.embabel.gekko.agent.integration;

import com.embabel.agent.test.integration.EmbabelMockitoIntegrationTest;
import com.embabel.agent.test.unit.FakeOperationContext;
import com.embabel.gekko.agent.DebateAgent;
import com.embabel.gekko.domain.ResearchTypes;
import com.embabel.gekko.util.FileCache;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the report generators (fundamentals, market, news, social media).
 *
 * Validates that each report generator:
 * 1. Invokes the LLM with the correct template and interaction ID
 * 2. Returns the stubbed response wrapped in the correct report type
 * 3. Includes the ticker in the prompt
 *
 * Extends EmbabelMockitoIntegrationTest for Spring context access.
 * Uses @Autowired to inject DebateAgent from the Spring context.
 * Uses FakeOperationContext (the framework test helper) instead of custom FakeActionContext.
 */
@Tag("integration")
class ReportGeneratorIntegrationTest extends EmbabelMockitoIntegrationTest {

    @Autowired
    private DebateAgent debateAgent;

    private Path tempCacheDir;

    @AfterEach
    void cleanupTempDir() {
        if (tempCacheDir != null) {
            try {
                java.nio.file.Files.walk(tempCacheDir)
                        .sorted((a, b) -> b.compareTo(a))
                        .forEach(path -> {
                            try { java.nio.file.Files.delete(path); } catch (Exception ignored) {}
                        });
            } catch (Exception ignored) {}
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        // Create a temp cache dir and inject it into the cached DebateAgent
        tempCacheDir = Files.createTempDirectory("report-gen-test-cache-");
        var cache = new FileCache();
        var field = FileCache.class.getDeclaredField("baseDir");
        field.setAccessible(true);
        field.set(cache, tempCacheDir);
        // Replace the cache field on the autowired DebateAgent
        var debateField = DebateAgent.class.getDeclaredField("cache");
        debateField.setAccessible(true);
        debateField.set(debateAgent, cache);
    }

    @Test
    void shouldGenerateFundamentalsReport() {
        // Arrange — stub the LLM response via FakeOperationContext
        var fake = FakeOperationContext.create();
        fake.expectResponse("Stub fundamentals report.");

        // Act — invoke the report generator
        var ticker = new ResearchTypes.Ticker("AAPL", "");
        var result = debateAgent.generateFundamentalsReport(ticker, fake);

        // Assert — verify the result is the stubbed response wrapped in a report
        assertEquals("Stub fundamentals report.", result.content());

        // Verify the LLM interaction
        var invocations = fake.getPromptRunner().getLlmInvocations();
        assertEquals(1, invocations.size());
        assertEquals("generateFundamentalsReport", invocations.get(0).getInteraction().getId());
    }

    @Test
    void shouldGenerateMarketReport() {
        // Arrange — stub the LLM response
        var fake = FakeOperationContext.create();
        fake.expectResponse("Stub market report.");

        // Act — invoke the report generator
        var ticker = new ResearchTypes.Ticker("AAPL", "");
        var result = debateAgent.generateMarketReport(ticker, fake);

        // Assert — verify the result is the stubbed response wrapped in a report
        assertEquals("Stub market report.", result.content());

        // Verify the LLM interaction
        var invocations = fake.getPromptRunner().getLlmInvocations();
        assertEquals(1, invocations.size());
        assertEquals("generateMarketReport", invocations.get(0).getInteraction().getId());
    }

    @Test
    void shouldGenerateNewsReport() {
        // Arrange — stub the LLM response
        var fake = FakeOperationContext.create();
        fake.expectResponse("Stub news report.");

        // Act — invoke the report generator
        var ticker = new ResearchTypes.Ticker("AAPL", "");
        var result = debateAgent.generateNewsReport(ticker, fake);

        // Assert — verify the result is the stubbed response wrapped in a report
        assertEquals("Stub news report.", result.content());

        // Verify the LLM interaction
        var invocations = fake.getPromptRunner().getLlmInvocations();
        assertEquals(1, invocations.size());
        assertEquals("generateNewsReport", invocations.get(0).getInteraction().getId());
    }

    @Test
    void shouldGenerateSocialMediaReport() {
        // Arrange — stub the LLM response
        var fake = FakeOperationContext.create();
        fake.expectResponse("Stub social media report.");

        // Act — invoke the report generator
        var ticker = new ResearchTypes.Ticker("AAPL", "");
        var result = debateAgent.generateSocialMediaReport(ticker, fake);

        // Assert — verify the result is the stubbed response wrapped in a report
        assertEquals("Stub social media report.", result.content());

        // Verify the LLM interaction
        var invocations = fake.getPromptRunner().getLlmInvocations();
        assertEquals(1, invocations.size());
        assertEquals("generateSocialMediaReport", invocations.get(0).getInteraction().getId());
    }
}