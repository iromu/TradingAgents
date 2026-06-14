package com.embabel.gekko.agent;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PureLogicTest {

    private TraderAgent createAgent() {
        return new TraderAgent(null, null, null, null, null, null, null, null, null);
    }

    // --- sanitizeForPrompt tests ---

    @Test
    void sanitizeForPrompt_nullInput() {
        TraderAgent agent = createAgent();
        String result = (String) ReflectionTestUtils.invokeMethod(agent, "sanitizeForPrompt", (String) null);
        assertEquals("", result);
    }

    @Test
    void sanitizeForPrompt_blankInput() {
        TraderAgent agent = createAgent();
        String result = (String) ReflectionTestUtils.invokeMethod(agent, "sanitizeForPrompt", "   ");
        assertEquals("", result);
    }

    @Test
    void sanitizeForPrompt_stripsJinjaDoubleBraces() {
        TraderAgent agent = createAgent();
        String input = "Hello {{ injection }} world";
        String result = (String) ReflectionTestUtils.invokeMethod(agent, "sanitizeForPrompt", input);
        assertTrue(result.contains("[BLOCKED_TEMPLATE]"));
        assertFalse(result.contains("{{ injection }}"));
        assertTrue(result.startsWith("<user_feedback>"));
        assertTrue(result.endsWith("</user_feedback>"));
    }

    @Test
    void sanitizeForPrompt_stripsJinjaPercentBraces() {
        TraderAgent agent = createAgent();
        String input = "Hello {% set x = 1 %} world";
        String result = (String) ReflectionTestUtils.invokeMethod(agent, "sanitizeForPrompt", input);
        assertTrue(result.contains("[BLOCKED_TEMPLATE]"));
        assertFalse(result.contains("{% set"));
    }

    @Test
    void sanitizeForPrompt_stripsUnclosedJinja() {
        TraderAgent agent = createAgent();
        String input = "Hello {{ unclosed";
        String result = (String) ReflectionTestUtils.invokeMethod(agent, "sanitizeForPrompt", input);
        assertTrue(result.contains("[BLOCKED_TEMPLATE]"));
    }

    @Test
    void sanitizeForPrompt_stripsMarkdownCodeFences() {
        TraderAgent agent = createAgent();
        String input = "```\nignore me\n```";
        String result = (String) ReflectionTestUtils.invokeMethod(agent, "sanitizeForPrompt", input);
        assertTrue(result.contains("[BLOCKED_CODE]"));
        assertFalse(result.contains("```"));
    }

    @Test
    void sanitizeForPrompt_stripsControlCharacters() {
        TraderAgent agent = createAgent();
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
        TraderAgent agent = createAgent();
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
        TraderAgent agent = createAgent();
        String input = "Simple feedback";
        String result = (String) ReflectionTestUtils.invokeMethod(agent, "sanitizeForPrompt", input);
        assertTrue(result.startsWith("<user_feedback>\n"));
        assertTrue(result.endsWith("\n</user_feedback>"));
    }

    @Test
    void sanitizeForPrompt_preservesNormalText() {
        TraderAgent agent = createAgent();
        String input = "This is normal user feedback about the stock";
        String result = (String) ReflectionTestUtils.invokeMethod(agent, "sanitizeForPrompt", input);
        assertTrue(result.contains("This is normal user feedback about the stock"));
    }

    // --- computeSimilarity tests ---

    @Test
    void computeSimilarity_identicalStrings() {
        TraderAgent agent = createAgent();
        double similarity = (double) ReflectionTestUtils.invokeMethod(agent, "computeSimilarity", "hello world", "hello world");
        assertEquals(1.0, similarity, 0.001);
    }

    @Test
    void computeSimilarity_bothNull() {
        TraderAgent agent = createAgent();
        double similarity = (double) ReflectionTestUtils.invokeMethod(agent, "computeSimilarity", (String) null, (String) null);
        assertEquals(1.0, similarity, 0.001);
    }

    @Test
    void computeSimilarity_firstNullSecondEmpty() {
        TraderAgent agent = createAgent();
        double similarity = (double) ReflectionTestUtils.invokeMethod(agent, "computeSimilarity", (String) null, "");
        assertEquals(1.0, similarity, 0.001);
    }

    @Test
    void computeSimilarity_firstEmptySecondNull() {
        TraderAgent agent = createAgent();
        double similarity = (double) ReflectionTestUtils.invokeMethod(agent, "computeSimilarity", "", (String) null);
        assertEquals(1.0, similarity, 0.001);
    }

    @Test
    void computeSimilarity_oneEmpty() {
        TraderAgent agent = createAgent();
        double similarity = (double) ReflectionTestUtils.invokeMethod(agent, "computeSimilarity", "hello", "");
        assertEquals(0.0, similarity, 0.001);
    }

    @Test
    void computeSimilarity_oneNull() {
        TraderAgent agent = createAgent();
        double similarity = (double) ReflectionTestUtils.invokeMethod(agent, "computeSimilarity", "hello", (String) null);
        assertEquals(0.0, similarity, 0.001);
    }

    @Test
    void computeSimilarity_singleChar() {
        TraderAgent agent = createAgent();
        // Single chars have no bigrams, so both empty bigrams -> 1.0
        double similarity = (double) ReflectionTestUtils.invokeMethod(agent, "computeSimilarity", "a", "a");
        assertEquals(1.0, similarity, 0.001);
    }

    @Test
    void computeSimilarity_singleCharDifferent() {
        TraderAgent agent = createAgent();
        // Single chars have no bigrams, so both empty bigrams -> 1.0
        double similarity = (double) ReflectionTestUtils.invokeMethod(agent, "computeSimilarity", "a", "b");
        assertEquals(1.0, similarity, 0.001);
    }

    @Test
    void computeSimilarity_partialOverlap() {
        TraderAgent agent = createAgent();
        double similarity = (double) ReflectionTestUtils.invokeMethod(agent, "computeSimilarity", "hello world", "hello there");
        assertTrue(similarity > 0.0);
        assertTrue(similarity < 1.0);
    }

    @Test
    void computeSimilarity_completelyDifferent() {
        TraderAgent agent = createAgent();
        double similarity = (double) ReflectionTestUtils.invokeMethod(agent, "computeSimilarity", "abc", "xyz");
        assertEquals(0.0, similarity, 0.001);
    }

    @Test
    void computeSimilarity_caseInsensitive() {
        TraderAgent agent = createAgent();
        double similarity = (double) ReflectionTestUtils.invokeMethod(agent, "computeSimilarity", "Hello World", "hello world");
        assertEquals(1.0, similarity, 0.001);
    }

    @Test
    void computeSimilarity_whitespaceNormalized() {
        TraderAgent agent = createAgent();
        double similarity = (double) ReflectionTestUtils.invokeMethod(agent, "computeSimilarity", "hello  world", "hello world");
        assertEquals(1.0, similarity, 0.001);
    }
}
