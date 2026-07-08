package com.embabel.gekko.agent.integration;

import com.embabel.agent.test.integration.EmbabelMockitoIntegrationTest;
import com.embabel.gekko.agent.FakeActionContext;
import com.embabel.gekko.agent.researchers.BearResearcher;
import com.embabel.gekko.agent.researchers.BullResearcher;
import com.embabel.gekko.domain.ResearchTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the researcher agents (BullResearcher and BearResearcher).
 *
 * Validates that each researcher:
 * 1. Invokes the LLM with the correct interaction ID
 * 2. Returns the stubbed response
 * 3. Includes the briefs and ticker in the prompt
 *
 * Extends EmbabelMockitoIntegrationTest for Spring context access.
 * Uses FakeActionContext (the framework test helper) for ActionContext-taking methods.
 */
@Tag("integration")
class ResearcherIntegrationTest extends EmbabelMockitoIntegrationTest {

    @Autowired
    private BullResearcher bullResearcher;

    @Autowired
    private BearResearcher bearResearcher;

    private ResearchTypes.DebateBriefs briefs;

    @BeforeEach
    void setUp() {
        briefs = new ResearchTypes.DebateBriefs(
                "Revenue grew 15% YoY.",
                "Bullish breakout above 200-day MA.",
                "Positive product launch news.",
                "Strong social sentiment."
        );
    }

    @Test
    void shouldInvokeBullResearcher() {
        // Arrange — stub the LLM response via FakeActionContext
        var fake = FakeActionContext.create();
        fake.getDelegate().expectResponse("# Bull Analyst\nBull argument about revenue growth.");

        // Act — invoke the bull researcher
        var result = bullResearcher.argue(briefs, Collections.emptyList(), fake.getActionContext());

        // Assert — verify the result contains the expected content
        assertTrue(result.contains("# Bull Analyst"));
        assertTrue(result.contains("Bull argument about revenue growth"));

        // Verify the LLM interaction
        var invocations = fake.getDelegate().getPromptRunner().getLlmInvocations();
        assertEquals(1, invocations.size());
        assertEquals("bullResearcher", invocations.get(0).getInteraction().getId());
    }

    @Test
    void shouldInvokeBearResearcher() {
        // Arrange — stub the LLM response via FakeActionContext
        var fake = FakeActionContext.create();
        fake.getDelegate().expectResponse("# Bear Analyst\nBear argument about valuation concerns.");

        // Act — invoke the bear researcher
        var result = bearResearcher.argue(briefs, Collections.emptyList(), fake.getActionContext());

        // Assert — verify the result contains the expected content
        assertTrue(result.contains("# Bear Analyst"));
        assertTrue(result.contains("Bear argument about valuation concerns"));

        // Verify the LLM interaction
        var invocations = fake.getDelegate().getPromptRunner().getLlmInvocations();
        assertEquals(1, invocations.size());
        assertEquals("bearResearcher", invocations.get(0).getInteraction().getId());
    }

    @Test
    void shouldIncludeBriefsInPrompt() {
        // Arrange — stub the LLM response
        var fake = FakeActionContext.create();
        fake.getDelegate().expectResponse("# Bull Analyst\nBull argument.");

        // Act — invoke the bull researcher
        bullResearcher.argue(briefs, Collections.emptyList(), fake.getActionContext());

        // Assert — verify the prompt includes the briefs
        var prompt = fake.getDelegate().getPromptRunner().getLlmInvocations().get(0).getPrompt();
        assertTrue(prompt.contains("Revenue grew 15% YoY"), "Prompt should contain fundamentals brief");
        assertTrue(prompt.contains("Bullish breakout"), "Prompt should contain market brief");
        assertTrue(prompt.contains("Positive product launch"), "Prompt should contain news brief");
        assertTrue(prompt.contains("Strong social sentiment"), "Prompt should contain social brief");
    }
}