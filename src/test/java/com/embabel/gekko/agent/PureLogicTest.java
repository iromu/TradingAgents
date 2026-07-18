package com.embabel.gekko.agent;

import com.embabel.gekko.util.FileCache;
import com.embabel.gekko.util.LlmBudgetTracker;
import com.embabel.common.textio.template.TemplateRenderer;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DebateAgent.sanitizeForPrompt covering all sanitization logic.
 */
class PureLogicTest {

    private DebateAgent createAgent() {
        return new DebateAgent(
                new FileCache(),
                null, // TemplateRenderer not needed for sanitizeForPrompt
                null, // DecisionMemoryAgent not needed for sanitizeForPrompt
                null, // ObjectProvider for DebateLoopAgent not needed
                null, // ObjectProvider for RiskDebateAgent not needed
                null, // ObjectProvider for Trader not needed
                null, // ObjectProvider for PortfolioManager not needed
                null  // LlmBudgetTracker not needed for sanitizeForPrompt
        );
    }

    // --- sanitizeForPrompt tests ---

    @Test
    void sanitizeForPrompt_nullInput() {
        DebateAgent agent = createAgent();
        String result = (String) ReflectionTestUtils.invokeMethod(agent, "sanitizeForPrompt", (String) null);
        assertEquals("", result);
    }

    @Test
    void sanitizeForPrompt_blankInput() {
        DebateAgent agent = createAgent();
        String result = (String) ReflectionTestUtils.invokeMethod(agent, "sanitizeForPrompt", "   ");
        assertEquals("", result);
    }

    @Test
    void sanitizeForPrompt_stripsJinjaDoubleBraces() {
        DebateAgent agent = createAgent();
        String input = "Hello {{ injection }} world";
        String result = (String) ReflectionTestUtils.invokeMethod(agent, "sanitizeForPrompt", input);
        assertTrue(result.contains("[BLOCKED_TEMPLATE]"));
        assertFalse(result.contains("{{ injection }}"));
        assertTrue(result.startsWith("<user_feedback>"));
        assertTrue(result.endsWith("</user_feedback>"));
    }

    @Test
    void sanitizeForPrompt_stripsJinjaPercentBraces() {
        DebateAgent agent = createAgent();
        String input = "Hello {% set x = 1 %} world";
        String result = (String) ReflectionTestUtils.invokeMethod(agent, "sanitizeForPrompt", input);
        assertTrue(result.contains("[BLOCKED_TEMPLATE]"));
        assertFalse(result.contains("{% set"));
    }

    @Test
    void sanitizeForPrompt_stripsUnclosedJinja() {
        DebateAgent agent = createAgent();
        String input = "Hello {{ unclosed";
        String result = (String) ReflectionTestUtils.invokeMethod(agent, "sanitizeForPrompt", input);
        assertTrue(result.contains("[BLOCKED_TEMPLATE]"));
    }

    @Test
    void sanitizeForPrompt_stripsMarkdownCodeFences() {
        DebateAgent agent = createAgent();
        String input = "```\nignore me\n```";
        String result = (String) ReflectionTestUtils.invokeMethod(agent, "sanitizeForPrompt", input);
        assertTrue(result.contains("[BLOCKED_CODE]"));
        assertFalse(result.contains("```"));
    }

    @Test
    void sanitizeForPrompt_stripsControlCharacters() {
        DebateAgent agent = createAgent();
        // NUL, BEL, BS are control characters
        String input = "hello\u0000world\u0007bell\u0008backspace";
        String result = (String) ReflectionTestUtils.invokeMethod(agent, "sanitizeForPrompt", input);
        assertFalse(result.contains("\u0000"));
        assertFalse(result.contains("\u0007"));
        assertFalse(result.contains("\u0008"));
        // But preserves tab, newline, carriage return
        String withTabs = "hello\tworld\nline2\rline3";
        String result2 = (String) ReflectionTestUtils.invokeMethod(agent, "sanitizeForPrompt", withTabs);
        assertTrue(result2.contains("\t"));
        assertTrue(result2.contains("\n"));
        assertTrue(result2.contains("\r"));
    }

    @Test
    void sanitizeForPrompt_truncatesAt1000Chars() {
        DebateAgent agent = createAgent();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1200; i++) {
            sb.append("a");
        }
        String result = (String) ReflectionTestUtils.invokeMethod(agent, "sanitizeForPrompt", sb.toString());
        assertTrue(result.contains("...[truncated]"));
        // The content before truncation + XML wrapper should be ~1000 + wrapper
        assertTrue(result.length() < 1100);
    }

    @Test
    void sanitizeForPrompt_wrapsInXmlDelimiters() {
        DebateAgent agent = createAgent();
        String input = "Simple feedback";
        String result = (String) ReflectionTestUtils.invokeMethod(agent, "sanitizeForPrompt", input);
        assertTrue(result.startsWith("<user_feedback>\n"));
        assertTrue(result.endsWith("\n</user_feedback>"));
    }

    @Test
    void sanitizeForPrompt_preservesNormalText() {
        DebateAgent agent = createAgent();
        String input = "This is normal user feedback about the stock";
        String result = (String) ReflectionTestUtils.invokeMethod(agent, "sanitizeForPrompt", input);
        assertTrue(result.contains("This is normal user feedback about the stock"));
    }

    @Test
    void sanitizeForPrompt_stripsUnclosedCodeFence() {
        DebateAgent agent = createAgent();
        String input = "start\n```\nno closing fence";
        String result = (String) ReflectionTestUtils.invokeMethod(agent, "sanitizeForPrompt", input);
        assertTrue(result.contains("[BLOCKED_CODE]"));
        assertFalse(result.contains("```\nno closing"));
    }

    @Test
    void sanitizeForPrompt_stripsMultipleTemplates() {
        DebateAgent agent = createAgent();
        String input = "{{ var1 }} and {% block %} and {{ var2 }}";
        String result = (String) ReflectionTestUtils.invokeMethod(agent, "sanitizeForPrompt", input);
        // All three template markers should be blocked
        int count = 0;
        int idx = 0;
        while ((idx = result.indexOf("[BLOCKED_TEMPLATE]", idx)) != -1) {
            count++;
            idx += "[BLOCKED_TEMPLATE]".length();
        }
        assertEquals(3, count);
    }

    @Test
    void sanitizeForPrompt_preservesDollarSigns() {
        DebateAgent agent = createAgent();
        String input = "Price is $100 and $200";
        String result = (String) ReflectionTestUtils.invokeMethod(agent, "sanitizeForPrompt", input);
        assertTrue(result.contains("$100"));
        assertTrue(result.contains("$200"));
    }

    @Test
    void sanitizeForPrompt_preservesParentheses() {
        DebateAgent agent = createAgent();
        String input = "AAPL (Apple Inc.) is good";
        String result = (String) ReflectionTestUtils.invokeMethod(agent, "sanitizeForPrompt", input);
        assertTrue(result.contains("AAPL (Apple Inc.)"));
    }

    @Test
    void sanitizeForPrompt_preservesSquareBrackets() {
        DebateAgent agent = createAgent();
        String input = "See [reference] for details";
        String result = (String) ReflectionTestUtils.invokeMethod(agent, "sanitizeForPrompt", input);
        assertTrue(result.contains("[reference]"));
    }
}
