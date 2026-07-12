package com.embabel.gekko.agent;

import com.embabel.agent.test.unit.FakeOperationContext;
import com.embabel.agent.test.unit.FakePromptRunner;
import com.embabel.agent.test.unit.LlmInvocation;
import com.embabel.gekko.agent.identity.InstrumentContextPromptContributor;
import com.embabel.gekko.agent.identity.InstrumentIdentityAgent;
import com.embabel.gekko.config.TraderAgentConfig;
import com.embabel.gekko.dataflows.YFinService;
import com.embabel.gekko.domain.ResearchTypes;
import com.embabel.gekko.util.FileCache;
import com.embabel.gekko.web.TradingHtmxController.TickerForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for OrchestratorAgent LLM-calling actions using FakeOperationContext + FakePromptRunner.
 * Verifies LLM role selection, interaction IDs, and prompt content.
 */
class OrchestratorAgentLLMTest {

    private FakeOperationContext ctx;
    private FakePromptRunner promptRunner;
    private OrchestratorAgent agent;

    private static ObjectProvider<com.embabel.gekko.dataflows.AlphaVantageService> mockAvProvider() {
        var provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        return provider;
    }

    @BeforeEach
    void setUp() {
        ctx = FakeOperationContext.create();
        promptRunner = ctx.getPromptRunner();

        FileCache cache = new FileCache();
        YFinService yFinService = new YFinService();
        InstrumentIdentityAgent identityAgent = new InstrumentIdentityAgent(yFinService, cache, mockAvProvider());

        agent = new OrchestratorAgent(
                cache,
                identityAgent,
                null, // memoryAgent
                null, // checkpointAgent
                new InstrumentContextPromptContributor(), // instrumentContextContributor
                null  // debateAgentProvider
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
