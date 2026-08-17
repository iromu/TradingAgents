package com.embabel.gekko.agent;

import com.embabel.gekko.util.PromptSanitizer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for PromptSanitizer covering all sanitization logic.
 */
class PureLogicTest {

    @Test
    void sanitizeForPrompt_nullInput() {
        assertThat(PromptSanitizer.sanitizeForPrompt(null)).isEmpty();
    }

    @Test
    void sanitizeForPrompt_blankInput() {
        assertThat(PromptSanitizer.sanitizeForPrompt("   ")).isEmpty();
    }

    @Test
    void sanitizeForPrompt_stripsJinjaDoubleBraces() {
        String input = "Hello {{ injection }} world";
        String result = PromptSanitizer.sanitizeForPrompt(input);
        assertThat(result).contains("[BLOCKED_TEMPLATE]").doesNotContain("{{ injection }}");
    }

    @Test
    void sanitizeForPrompt_stripsJinjaPercentBraces() {
        String input = "Hello {% set x = 1 %} world";
        String result = PromptSanitizer.sanitizeForPrompt(input);
        assertThat(result).contains("[BLOCKED_TEMPLATE]").doesNotContain("{% set");
    }

    @Test
    void sanitizeForPrompt_stripsUnclosedJinja() {
        String input = "Hello {{ unclosed";
        String result = PromptSanitizer.sanitizeForPrompt(input);
        assertThat(result).contains("[BLOCKED_TEMPLATE]");
    }

    @Test
    void sanitizeForPrompt_stripsMarkdownCodeFences() {
        String input = "```\nignore me\n```";
        String result = PromptSanitizer.sanitizeForPrompt(input);
        assertThat(result).contains("[BLOCKED_CODE]").doesNotContain("```");
    }

    @Test
    void sanitizeForPrompt_stripsControlCharacters() {
        String input = "hello\u0000world\u0007bell\u0008backspace";
        String result = PromptSanitizer.sanitizeForPrompt(input);
        assertThat(result).doesNotContain("\u0000").doesNotContain("\u0007").doesNotContain("\u0008");

        String withTabs = "hello\tworld\nline2\rline3";
        String result2 = PromptSanitizer.sanitizeForPrompt(withTabs);
        assertThat(result2).contains("\t").contains("\n").contains("\r");
    }

    @Test
    void sanitizeForPrompt_truncatesOversized() {
        String input = "a".repeat(12_000);
        String result = PromptSanitizer.sanitizeForPrompt(input);
        assertThat(result.length()).isLessThanOrEqualTo(10_000 + 14);
    }

    @Test
    void wrapUserFeedback_wrapsInXmlDelimiters() {
        String result = PromptSanitizer.wrapUserFeedback("Simple feedback");
        assertThat(result).startsWith("<user_feedback>\n").endsWith("\n</user_feedback>");
    }

    @Test
    void wrapUserFeedback_emptyReturnsEmpty() {
        assertThat(PromptSanitizer.wrapUserFeedback("")).isEmpty();
        assertThat(PromptSanitizer.wrapUserFeedback(null)).isEmpty();
    }

    @Test
    void sanitizeForPrompt_preservesNormalText() {
        String input = "This is normal user feedback about the stock";
        assertThat(PromptSanitizer.sanitizeForPrompt(input)).isEqualTo(input);
    }

    @Test
    void sanitizeForPrompt_stripsUnclosedCodeFence() {
        String input = "start\n```\nno closing fence";
        String result = PromptSanitizer.sanitizeForPrompt(input);
        assertThat(result).contains("[BLOCKED_CODE]").doesNotContain("```\nno closing");
    }

    @Test
    void sanitizeForPrompt_stripsMultipleTemplates() {
        String input = "{{ var1 }} and {% block %} and {{ var2 }}";
        String result = PromptSanitizer.sanitizeForPrompt(input);
        int count = 0;
        int idx = 0;
        while ((idx = result.indexOf("[BLOCKED_TEMPLATE]", idx)) != -1) {
            count++;
            idx += "[BLOCKED_TEMPLATE]".length();
        }
        assertThat(count).isEqualTo(3);
    }

    @Test
    void sanitizeForPrompt_preservesDollarSigns() {
        String input = "Price is $100 and $200";
        assertThat(PromptSanitizer.sanitizeForPrompt(input)).contains("$100").contains("$200");
    }

    @Test
    void sanitizeForPrompt_preservesParentheses() {
        String input = "AAPL (Apple Inc.) is good";
        assertThat(PromptSanitizer.sanitizeForPrompt(input)).contains("AAPL (Apple Inc.)");
    }

    @Test
    void sanitizeForPrompt_preservesSquareBrackets() {
        String input = "See [reference] for details";
        assertThat(PromptSanitizer.sanitizeForPrompt(input)).contains("[reference]");
    }
}
