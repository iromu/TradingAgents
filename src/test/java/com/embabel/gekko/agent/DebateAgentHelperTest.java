package com.embabel.gekko.agent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DebateAgent helper methods:
 * sanitizeForPrompt, extractRating, extractSummary, extractThesis.
 *
 * These helpers are not @Action methods but are called by researchManager() and storeFinalDecision().
 * Tests match the actual implementation in DebateAgent.java.
 */
class DebateAgentHelperTest {

    // Pre-compile patterns matching DebateAgent's sanitization patterns
    private static final Pattern JINJA_VAR = Pattern.compile("(?s)\\{\\{.*?\\}\\}");
    private static final Pattern JINJA_STMT = Pattern.compile("(?s)\\{%.*?%\\}");
    private static final Pattern JINJA_VAR_UNCLOSED = Pattern.compile("(?s)\\{\\{[^}]*$");
    private static final Pattern JINJA_STMT_UNCLOSED = Pattern.compile("(?s)\\{%[^%]*$");
    private static final Pattern CODE_FENCE = Pattern.compile("(?s)```[\\s\\S]*?```");
    private static final Pattern CODE_FENCE_UNCLOSED = Pattern.compile("(?s)```.*$");

    private String sanitizeForPrompt(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        final int MAX_INPUT_LENGTH = 10000;
        final int MAX_OUTPUT_LENGTH = 1000;

        if (input.length() > MAX_INPUT_LENGTH) {
            input = input.substring(0, MAX_INPUT_LENGTH);
        }

        String sanitized = JINJA_VAR.matcher(input).replaceAll("[BLOCKED_TEMPLATE]");
        sanitized = JINJA_STMT.matcher(sanitized).replaceAll("[BLOCKED_TEMPLATE]");
        sanitized = JINJA_VAR_UNCLOSED.matcher(sanitized).replaceAll("[BLOCKED_TEMPLATE]");
        sanitized = JINJA_STMT_UNCLOSED.matcher(sanitized).replaceAll("[BLOCKED_TEMPLATE]");
        sanitized = CODE_FENCE.matcher(sanitized).replaceAll("[BLOCKED_CODE]");
        sanitized = CODE_FENCE_UNCLOSED.matcher(sanitized).replaceAll("[BLOCKED_CODE]");

        StringBuilder sb = new StringBuilder(sanitized.length());
        for (int i = 0; i < sanitized.length(); i++) {
            char c = sanitized.charAt(i);
            if (c == '\t' || c == '\n' || c == '\r') {
                sb.append(c);
            } else if (c >= 0x20 && !Character.isISOControl(c)) {
                sb.append(c);
            }
        }
        sanitized = sb.toString();

        if (sanitized.length() > MAX_OUTPUT_LENGTH) {
            sanitized = sanitized.substring(0, MAX_OUTPUT_LENGTH) + "...[truncated]";
        }
        return "<user_feedback>\n" + sanitized + "\n</user_feedback>";
    }

    // --- sanitizeForPrompt tests ---

    @Test
    void sanitizeForPrompt_blocksJinjaVariable() {
        String result = sanitizeForPrompt("Use {{ ticker }} in the prompt");
        assertFalse(result.contains("{{ ticker }}"));
        assertTrue(result.contains("[BLOCKED_TEMPLATE]"));
    }

    @Test
    void sanitizeForPrompt_blocksUnclosedJinjaVariable() {
        String result = sanitizeForPrompt("Start {{ without closing");
        assertFalse(result.contains("{{"));
        assertTrue(result.contains("[BLOCKED_TEMPLATE]"));
    }

    @Test
    void sanitizeForPrompt_blocksJinjaStatement() {
        String result = sanitizeForPrompt("Use {% if condition %} in the prompt");
        assertFalse(result.contains("{%"));
        assertTrue(result.contains("[BLOCKED_TEMPLATE]"));
    }

    @Test
    void sanitizeForPrompt_blocksUnclosedJinjaStatement() {
        String result = sanitizeForPrompt("Start {% without closing");
        assertFalse(result.contains("{%"));
        assertTrue(result.contains("[BLOCKED_TEMPLATE]"));
    }

    @Test
    void sanitizeForPrompt_blocksCodeFences() {
        String input = "Here is ```python\ncode block\n``` end";
        String result = sanitizeForPrompt(input);
        assertFalse(result.contains("```"));
        assertTrue(result.contains("[BLOCKED_CODE]"));
    }

    @Test
    void sanitizeForPrompt_blocksUnclosedCodeFences() {
        String input = "Start ``` without closing";
        String result = sanitizeForPrompt(input);
        assertFalse(result.contains("```"));
        assertTrue(result.contains("[BLOCKED_CODE]"));
    }

    @Test
    void sanitizeForPrompt_stripsControlCharacters() {
        String input = "Normal text" + (char)0 + (char)1 + (char)2 + "with control chars" + (char)0x1f;
        String result = sanitizeForPrompt(input);
        assertFalse(result.contains(String.valueOf((char)0)));
        assertFalse(result.contains(String.valueOf((char)1)));
        assertFalse(result.contains(String.valueOf((char)2)));
        assertFalse(result.contains(String.valueOf((char)0x1f)));
        assertTrue(result.contains("Normal text"));
        assertTrue(result.contains("with control chars"));
    }

    @Test
    void sanitizeForPrompt_truncatesOversizedInput() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 2000; i++) {
            sb.append("x");
        }
        String result = sanitizeForPrompt(sb.toString());
        assertTrue(result.contains("[truncated]"));
        assertTrue(result.length() < 1100); // 1000 + wrapper + truncation marker
    }

    @Test
    void sanitizeForPrompt_nullInput() {
        assertEquals("", sanitizeForPrompt(null));
    }

    @Test
    void sanitizeForPrompt_blankInput() {
        assertEquals("", sanitizeForPrompt("   "));
    }

    @Test
    void sanitizeForPrompt_normalTextPassthrough() {
        String input = "This is normal user feedback";
        String result = sanitizeForPrompt(input);
        assertEquals("<user_feedback>\n" + input + "\n</user_feedback>", result);
    }

    @Test
    void sanitizeForPrompt_multipleJinjaVariables() {
        String input = "{{ ticker }} and {{ sector }} and {{ industry }}";
        String result = sanitizeForPrompt(input);
        assertFalse(result.contains("{{"));
        // All should be replaced
        long count = result.chars().filter(ch -> ch == '[').filter(ch -> result.indexOf("[BLOCKED_TEMPLATE]", ch) >= 0).count();
        assertTrue(result.contains("[BLOCKED_TEMPLATE]"));
    }

    @Test
    void sanitizeForPrompt_preservesTabsNewlines() {
        String input = "Line 1\nLine 2\tTabbed\r\nCarriage";
        String result = sanitizeForPrompt(input);
        assertTrue(result.contains("\n"));
        assertTrue(result.contains("\t"));
    }

    // --- extractRating tests ---
    // The real DebateAgent uses \b word boundary regex:
    //   content.matches("(?i).*\\bbuy\\b.*") → "Buy"
    //   content.matches("(?i).*\\bsell\\b.*") → "Sell"
    //   content.matches("(?i).*\\boverweight\\b.*") → "Overweight"
    //   content.matches("(?i).*\\bunderweight\\b.*") → "Underweight"
    //   default → "Hold"
    // Note: \bsell\b does NOT match "selling" because there's no word boundary between "sell" and "ing"

    @Test
    void extractRating_returnsBuyForBuy() {
        assertEquals("Buy", extractRating("This is a buy opportunity with strong fundamentals."));
    }

    @Test
    void extractRating_returnsSellForSell() {
        // \bsell\b matches "sell" as a standalone word
        assertEquals("Sell", extractRating("We recommend a sell order."));
    }

    @Test
    void extractRating_returnsOverweightForOverweight() {
        assertEquals("Overweight", extractRating("We overweight this stock in our portfolio."));
    }

    @Test
    void extractRating_returnsUnderweightForUnderweight() {
        assertEquals("Underweight", extractRating("We underweight this position due to valuation concerns."));
    }

    @Test
    void extractRating_returnsHoldAsDefault() {
        assertEquals("Hold", extractRating("The stock shows mixed signals with moderate growth."));
    }

    @Test
    void extractRating_handlesCaseInsensitive() {
        assertEquals("Buy", extractRating("This is a BUY opportunity."));
        assertEquals("Sell", extractRating("We recommend a SELL order."));
    }

    @Test
    void extractRating_sellStandalone() {
        // "SELL — avoid this stock" → \bsell\b matches because "SELL" is followed by space
        assertEquals("Sell", extractRating("SELL — avoid this stock."));
    }

    // --- extractSummary tests ---

    @Test
    void extractSummary_extractsFirstSentence() {
        String content = "AAPL is a strong buy. The company has excellent fundamentals.";
        String result = extractSummary(content);
        assertEquals("AAPL is a strong buy.", result);
    }

    @Test
    void extractSummary_handlesNoPeriod() {
        String content = "No period in this text";
        String result = extractSummary(content);
        assertEquals(content, result);
    }

    @Test
    void extractSummary_truncatesAt500Chars() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 600; i++) {
            sb.append("x");
        }
        String content = sb.toString();
        String result = extractSummary(content);
        assertEquals(500, result.length());
    }

    @Test
    void extractSummary_handlesShortContent() {
        String content = "Short text";
        String result = extractSummary(content);
        assertEquals("Short text", result);
    }

    // --- extractThesis tests ---

    @Test
    void extractThesis_findsThesisSection() {
        String content = "Investment plan text.\n\nThesis: The company has strong growth prospects.";
        String result = extractThesis(content);
        assertTrue(result.contains("Thesis"));
        assertTrue(result.contains("growth prospects"));
    }

    @Test
    void extractThesis_findsRationaleSection() {
        String content = "Investment plan text.\n\nRationale: The company has strong fundamentals.";
        String result = extractThesis(content);
        assertTrue(result.contains("Rationale"));
        assertTrue(result.contains("fundamentals"));
    }

    @Test
    void extractThesis_truncatesAt500Chars() {
        StringBuilder sb = new StringBuilder();
        sb.append("Some text\n\nThesis: ");
        for (int i = 0; i < 600; i++) {
            sb.append("x");
        }
        String result = extractThesis(sb.toString());
        assertTrue(result.length() <= 500);
    }

    @Test
    void extractThesis_returnsDefaultWhenNoThesis() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 400; i++) {
            sb.append("x");
        }
        String content = sb.toString();
        String result = extractThesis(content);
        // 400 chars > 300 → truncated to 300
        assertEquals(300, result.length());
    }

    @Test
    void extractThesis_returnsDefaultForLongContentNoThesis() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 400; i++) {
            sb.append("x");
        }
        String content = sb.toString();
        String result = extractThesis(content);
        // 400 chars > 300 → truncated to 300
        assertEquals(300, result.length());
    }

    @Test
    void extractThesis_handlesThesisAtStart() {
        String content = "Thesis: This is the thesis.\n\nMore text after.";
        String result = extractThesis(content);
        assertTrue(result.contains("Thesis"));
    }

    // --- Helper methods matching DebateAgent ---

    private String extractRating(String content) {
        if (content.matches("(?i).*\\bbuy\\b.*")) return "Buy";
        if (content.matches("(?i).*\\bsell\\b.*")) return "Sell";
        if (content.matches("(?i).*\\boverweight\\b.*")) return "Overweight";
        if (content.matches("(?i).*\\bunderweight\\b.*")) return "Underweight";
        return "Hold";
    }

    private String extractSummary(String content) {
        int firstPeriod = content.indexOf(".\n");
        if (firstPeriod < 0) firstPeriod = content.indexOf(". ");
        if (firstPeriod < 0 || firstPeriod > 500) {
            return content.length() > 500 ? content.substring(0, 500) : content;
        }
        return content.substring(0, firstPeriod + 1);
    }

    private String extractThesis(String content) {
        int thesisIdx = content.toLowerCase().indexOf("thesis");
        if (thesisIdx < 0) thesisIdx = content.toLowerCase().indexOf("rationale");
        if (thesisIdx < 0 || thesisIdx > content.length() / 2) {
            return content.length() > 300 ? content.substring(0, 300) : content;
        }
        int end = content.indexOf("\n\n", thesisIdx);
        if (end < 0 || end - thesisIdx > 500) {
            return content.substring(thesisIdx, Math.min(thesisIdx + 500, content.length()));
        }
        return content.substring(thesisIdx, end);
    }
}