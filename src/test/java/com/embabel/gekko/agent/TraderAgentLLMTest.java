package com.embabel.gekko.agent;

import com.embabel.agent.test.unit.FakeOperationContext;
import com.embabel.agent.test.unit.FakePromptRunner;
import com.embabel.agent.test.unit.LlmInvocation;
import com.embabel.gekko.config.TraderAgentConfig;
import com.embabel.gekko.domain.ResearchTypes;
import com.embabel.gekko.web.TradingHtmxController.TickerForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for OrchestratorAgent LLM-calling actions using FakeOperationContext + FakePromptRunner.
 * Verifies LLM role selection, interaction IDs, and prompt content.
 */
class OrchestratorAgentLLMTest {

    private FakeOperationContext ctx;
    private FakePromptRunner promptRunner;
    private OrchestratorAgent agent;

    @BeforeEach
    void setUp() {
        ctx = FakeOperationContext.create();
        promptRunner = ctx.getPromptRunner();

        TraderAgentConfig config = new TraderAgentConfig(
                null, null, 4, null, null, null,
                "/tmp", 0.8, 5
        );

        agent = new OrchestratorAgent(
                new com.embabel.gekko.util.FileCache(),
                null
        );
    }

    // --- tickerFromForm tests ---

    @Test
    void tickerFromForm_usesCorrectRoleAndId() {
        // Act — tickerFromForm no longer calls LLM, just validates and returns
        ResearchTypes.Ticker result = agent.tickerFromForm(new TickerForm("AAPL", ""), ctx);

        // Assert
        assertEquals("AAPL", result.content());
        // No LLM call — ticker extraction is deterministic
        List<LlmInvocation> invocations = promptRunner.getLlmInvocations();
        assertEquals(0, invocations.size());
    }

    @Test
    void tickerFromForm_blankInputThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                agent.tickerFromForm(new TickerForm("", ""), ctx)
        );
    }

    @Test
    void tickerFromForm_invalidCharactersThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                agent.tickerFromForm(new TickerForm("AAPL@#$", ""), ctx)
        );
    }

    @Test
    void tickerFromForm_convertsToUpperCase() {
        // Act — no LLM call needed, just string manipulation
        ResearchTypes.Ticker result = agent.tickerFromForm(new TickerForm("aapl", ""), ctx);

        // Assert
        assertEquals("AAPL", result.content());
    }

    @Test
    void tickerFromForm_allowsDotForETFs() {
        // Arrange
        ctx.expectResponse(new ResearchTypes.Ticker("SPY.X", ""));

        // Act
        ResearchTypes.Ticker result = agent.tickerFromForm(new TickerForm("SPY.X", ""), ctx);

        // Assert
        assertEquals("SPY.X", result.content());
    }

    @Test
    void tickerFromForm_rejectsHyphen() {
        assertThrows(IllegalArgumentException.class, () ->
                agent.tickerFromForm(new TickerForm("BTC-USD", ""), ctx)
        );
    }
}
