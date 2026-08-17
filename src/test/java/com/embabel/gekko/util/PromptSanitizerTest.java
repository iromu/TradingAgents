package com.embabel.gekko.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PromptSanitizerTest {

    @Test
    void nullInputReturnsEmpty() {
        assertThat(PromptSanitizer.sanitizeForPrompt(null)).isEmpty();
    }

    @Test
    void blankInputReturnsEmpty() {
        assertThat(PromptSanitizer.sanitizeForPrompt("   ")).isEmpty();
    }

    @Test
    void plainTextPassesThrough() {
        assertThat(PromptSanitizer.sanitizeForPrompt("NVDA is a strong buy")).isEqualTo("NVDA is a strong buy");
    }

    @Test
    void jinjaVariableIsNeutralized() {
        String result = PromptSanitizer.sanitizeForPrompt("hello {{ malicious }} world");
        assertThat(result).doesNotContain("{{").doesNotContain("}}");
        assertThat(result).contains("[BLOCKED_TEMPLATE]");
    }

    @Test
    void jinjaStatementIsNeutralized() {
        String result = PromptSanitizer.sanitizeForPrompt("{% if x %}y{% endif %}");
        assertThat(result).doesNotContain("{%").doesNotContain("%}");
        assertThat(result).contains("[BLOCKED_TEMPLATE]");
    }

    @Test
    void unclosedJinjaVariableIsNeutralized() {
        String result = PromptSanitizer.sanitizeForPrompt("text {{ unclosed var");
        assertThat(result).doesNotContain("{{");
        assertThat(result).contains("[BLOCKED_TEMPLATE]");
    }

    @Test
    void codeFenceIsNeutralized() {
        String result = PromptSanitizer.sanitizeForPrompt("before ```python\nprint(1)\n``` after");
        assertThat(result).doesNotContain("```");
        assertThat(result).contains("[BLOCKED_CODE]");
    }

    @Test
    void scriptTagIsNeutralized() {
        String result = PromptSanitizer.sanitizeForPrompt("<script>alert('xss')</script>");
        assertThat(result).doesNotContain("<script");
        assertThat(result).contains("[BLOCKED_HTML]");
    }

    @Test
    void iframeTagIsNeutralized() {
        String result = PromptSanitizer.sanitizeForPrompt("<iframe src='evil.com'></iframe>");
        assertThat(result).doesNotContain("<iframe");
    }

    @Test
    void eventHandlerAttributeIsNeutralized() {
        String result = PromptSanitizer.sanitizeForPrompt("<div onclick='alert(1)'>click</div>");
        assertThat(result).doesNotContain("onclick=");
    }

    @Test
    void controlCharactersAreStripped() {
        String input = "line1\u0000\u0001line2";
        String result = PromptSanitizer.sanitizeForPrompt(input);
        assertThat(result).doesNotContain("\u0000").doesNotContain("\u0001");
        assertThat(result).contains("line1").contains("line2");
    }

    @Test
    void tabsAndNewlinesArePreserved() {
        String input = "line1\nline2\ttabbed";
        assertThat(PromptSanitizer.sanitizeForPrompt(input)).isEqualTo(input);
    }

    @Test
    void oversizedInputIsTruncated() {
        String input = "A".repeat(20_000);
        String result = PromptSanitizer.sanitizeForPrompt(input);
        // Input is truncated to MAX_INPUT_LENGTH (10_000) before processing;
        // output cap is also 10_000 so no "...[truncated]" suffix is appended.
        assertThat(result.length()).isEqualTo(10_000);
        assertThat(result).doesNotContain("...[truncated]");
    }

    @Test
    void wrapUserFeedbackWrapsInXml() {
        String result = PromptSanitizer.wrapUserFeedback("looks good to me");
        assertThat(result).startsWith("<user_feedback>").endsWith("</user_feedback>");
        assertThat(result).contains("looks good to me");
    }

    @Test
    void wrapUserFeedbackEmptyReturnsEmpty() {
        assertThat(PromptSanitizer.wrapUserFeedback("")).isEmpty();
        assertThat(PromptSanitizer.wrapUserFeedback(null)).isEmpty();
    }

    @Test
    void htmlCommentIsRemoved() {
        String result = PromptSanitizer.sanitizeForPrompt("before <!-- hidden --> after");
        assertThat(result).doesNotContain("<!--");
        assertThat(result).contains("before").contains("after");
    }

    @Test
    void combinedInjectionAttemptIsNeutralized() {
        String payload = "{{ config.secret }}<script>steal()</script>\u0000{% set x = 1 %}";
        String result = PromptSanitizer.sanitizeForPrompt(payload);
        assertThat(result).doesNotContain("{{").doesNotContain("<script").doesNotContain("{%").doesNotContain("\u0000");
    }
}
