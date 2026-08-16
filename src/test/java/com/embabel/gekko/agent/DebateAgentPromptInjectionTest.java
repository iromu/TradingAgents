package com.embabel.gekko.agent;

import com.embabel.gekko.util.FileCache;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for prompt injection fixes in DebateAgent:
 * - sanitizeValue() for non-feedback template values
 * - extractRating() priority-based extraction
 * - history and portfolio_decision sanitization
 */
class DebateAgentPromptInjectionTest {

    private DebateAgent createAgent() {
        return new DebateAgent(
                new FileCache(), null, null, null, null, null, null, null, null
        );
    }

    // --- sanitizeValue tests (no XML wrapper) ---

    @Test
    void sanitizeValue_nullInput() {
        DebateAgent agent = createAgent();
        String result = (String) ReflectionTestUtils.invokeMethod(agent, "sanitizeValue", (String) null);
        assertEquals("", result);
    }

    @Test
    void sanitizeValue_blankInput() {
        DebateAgent agent = createAgent();
        String result = (String) ReflectionTestUtils.invokeMethod(agent, "sanitizeValue", "   ");
        assertEquals("", result);
    }

    @Test
    void sanitizeValue_blocksJinjaVariables() {
        DebateAgent agent = createAgent();
        String input = "{{ ticker }} is great";
        String result = (String) ReflectionTestUtils.invokeMethod(agent, "sanitizeValue", input);
        assertFalse(result.contains("{{"));
        assertTrue(result.contains("[BLOCKED_TEMPLATE]"));
    }

    @Test
    void sanitizeValue_blocksJinjaStatements() {
        DebateAgent agent = createAgent();
        String input = "{% if true %}hello{% endif %}";
        String result = (String) ReflectionTestUtils.invokeMethod(agent, "sanitizeValue", input);
        assertFalse(result.contains("{%"));
        assertTrue(result.contains("[BLOCKED_TEMPLATE]"));
    }

    @Test
    void sanitizeValue_blocksCodeFences() {
        DebateAgent agent = createAgent();
        String input = "```\nmalicious code\n```";
        String result = (String) ReflectionTestUtils.invokeMethod(agent, "sanitizeValue", input);
        assertFalse(result.contains("```"));
        assertTrue(result.contains("[BLOCKED_CODE]"));
    }

    @Test
    void sanitizeValue_noXmlWrapper() {
        DebateAgent agent = createAgent();
        String input = "Normal text";
        String result = (String) ReflectionTestUtils.invokeMethod(agent, "sanitizeValue", input);
        assertFalse(result.contains("<user_feedback>"));
        assertFalse(result.contains("</user_feedback>"));
        assertEquals("Normal text", result);
    }

    @Test
    void sanitizeValue_normalTextPassthrough() {
        DebateAgent agent = createAgent();
        String input = "Debate round 1: Agent A says buy";
        String result = (String) ReflectionTestUtils.invokeMethod(agent, "sanitizeValue", input);
        assertEquals("Debate round 1: Agent A says buy", result);
    }

    @Test
    void sanitizeValue_stripsControlChars() {
        DebateAgent agent = createAgent();
        String input = "hello\u0000world";
        String result = (String) ReflectionTestUtils.invokeMethod(agent, "sanitizeValue", input);
        assertFalse(result.contains("\u0000"));
        assertEquals("helloworld", result);
    }

    @Test
    void sanitizeValue_truncatesOversized() {
        DebateAgent agent = createAgent();
        String input = "a".repeat(1200);
        String result = (String) ReflectionTestUtils.invokeMethod(agent, "sanitizeValue", input);
        assertTrue(result.contains("[truncated]"));
    }

    // --- extractRating priority tests ---

    @Test
    void extractRating_buyWinsOverOverweight() {
        DebateAgent agent = createAgent();
        // "overweight" appears first but "buy" has higher priority
        String input = "We are overweight this position and recommend a buy.";
        String result = (String) ReflectionTestUtils.invokeMethod(agent, "extractRating", input);
        assertEquals("Buy", result);
    }

    @Test
    void extractRating_sellWinsOverUnderweight() {
        DebateAgent agent = createAgent();
        String input = "Underweight position, time to sell.";
        String result = (String) ReflectionTestUtils.invokeMethod(agent, "extractRating", input);
        assertEquals("Sell", result);
    }

    @Test
    void extractRating_overweightWinsOverHold() {
        DebateAgent agent = createAgent();
        String input = "Hold but overweight relative to benchmark.";
        String result = (String) ReflectionTestUtils.invokeMethod(agent, "extractRating", input);
        assertEquals("Overweight", result);
    }

    @Test
    void extractRating_underweightWinsOverHold() {
        DebateAgent agent = createAgent();
        String input = "Hold but underweight due to risk.";
        String result = (String) ReflectionTestUtils.invokeMethod(agent, "extractRating", input);
        assertEquals("Underweight", result);
    }

    @Test
    void extractRating_buyAlone() {
        DebateAgent agent = createAgent();
        String result = (String) ReflectionTestUtils.invokeMethod(agent, "extractRating", "Strong buy signal.");
        assertEquals("Buy", result);
    }

    @Test
    void extractRating_sellAlone() {
        DebateAgent agent = createAgent();
        String result = (String) ReflectionTestUtils.invokeMethod(agent, "extractRating", "SELL — avoid this stock.");
        assertEquals("Sell", result);
    }

    @Test
    void extractRating_holdDefault() {
        DebateAgent agent = createAgent();
        String result = (String) ReflectionTestUtils.invokeMethod(agent, "extractRating", "No clear signal, mixed fundamentals.");
        assertEquals("Hold", result);
    }

    @Test
    void extractRating_caseInsensitive() {
        DebateAgent agent = createAgent();
        String result = (String) ReflectionTestUtils.invokeMethod(agent, "extractRating", "BUY NOW.");
        assertEquals("Buy", result);
    }

    @Test
    void extractRating_wordBoundaryNoFalsePositive() {
        DebateAgent agent = createAgent();
        // "selling" should NOT match "sell" due to word boundary
        // But wait - \bsell\b won't match "selling" because there's no boundary between l and i
        String result = (String) ReflectionTestUtils.invokeMethod(agent, "extractRating", "Selling off other positions but holding this one.");
        assertEquals("Hold", result);
    }

    @Test
    void extractRating_emptyInput() {
        DebateAgent agent = createAgent();
        String result = (String) ReflectionTestUtils.invokeMethod(agent, "extractRating", "");
        assertEquals("Hold", result);
    }

    // --- extractRating buy/sell conflict tests ---

    @Test
    void extractRating_buyAndSellConflict_strongBuyWins() {
        DebateAgent agent = createAgent();
        String input = "We recommend a strong buy despite some sell pressure from competitors.";
        String result = (String) ReflectionTestUtils.invokeMethod(agent, "extractRating", input);
        assertEquals("Buy", result);
    }

    @Test
    void extractRating_buyAndSellConflict_strongSellWins() {
        DebateAgent agent = createAgent();
        String input = "Despite early buy interest, we see a strong sell signal forming.";
        String result = (String) ReflectionTestUtils.invokeMethod(agent, "extractRating", input);
        assertEquals("Sell", result);
    }

    @Test
    void extractRating_buyAndSellConflict_defaultsToBuy() {
        DebateAgent agent = createAgent();
        String input = "Some analysts want to buy while others want to sell.";
        String result = (String) ReflectionTestUtils.invokeMethod(agent, "extractRating", input);
        assertEquals("Buy", result);
    }

    // --- sanitizeValue unclosed code fence tests ---

    @Test
    void sanitizeValue_unclosedCodeFence_nonGreedyMatch() {
        DebateAgent agent = createAgent();
        String input = "Here is some text\n```\nunclosed fence content";
        String result = (String) ReflectionTestUtils.invokeMethod(agent, "sanitizeValue", input);
        assertFalse(result.contains("```"));
        assertTrue(result.contains("[BLOCKED_CODE]"));
    }

    @Test
    void sanitizeValue_multipleUnclosedFences() {
        DebateAgent agent = createAgent();
        String input = "first ```\ncontent one\nsecond ```\ncontent two";
        String result = (String) ReflectionTestUtils.invokeMethod(agent, "sanitizeValue", input);
        assertFalse(result.contains("```"));
    }
}
