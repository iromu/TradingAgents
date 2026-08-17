package com.embabel.gekko.util;

import java.util.regex.Pattern;

/**
 * Shared prompt-input sanitizer. Neutralizes Jinja template syntax, HTML/script
 * content, and control characters so external or user-derived text cannot inject
 * template payloads into agent prompts.
 */
public final class PromptSanitizer {

    private static final Pattern JINJA_VAR = Pattern.compile("(?s)\\{\\{.*?\\}\\}");
    private static final Pattern JINJA_STMT = Pattern.compile("(?s)\\{%.*?%\\}");
    private static final Pattern JINJA_VAR_UNCLOSED = Pattern.compile("(?s)\\{\\{[^}]*$");
    private static final Pattern JINJA_STMT_UNCLOSED = Pattern.compile("(?s)\\{%[^%]*$");
    private static final Pattern CODE_FENCE = Pattern.compile("(?s)```[\\s\\S]*?```");
    private static final Pattern CODE_FENCE_UNCLOSED = Pattern.compile("(?s)```.*?$");
    private static final Pattern HTML_TAG = Pattern.compile("<(/?)(?:script|style|iframe|object|embed|form|input|button|select|textarea|img|svg|link|meta|base)[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern HTML_COMMENT = Pattern.compile("<!--.*?-->", Pattern.DOTALL);
    private static final Pattern EVENT_HANDLER = Pattern.compile("\\son(?:click|load|error|submit|change|mouseover|mouseout|focus|blur|keydown|keyup|keypress|dblclick|contextmenu|drag|drop|paste|copy|cut|input|reset|select|toggle|wheel|abort|animationend|animationstart|animationiteration|canplay|canplaythrough|durationchange|emptied|ended|loadeddata|loadedmetadata|loadstart|pause|play|playing|progress|ratechange|seeked|seeking|stalled|suspend|timeupdate|volumechange|waiting|message|pop|show|storage|beforeunload|hashchange|languagechange|offline|online|pagehide|pageshow|resize|scroll|search|unload)\\s*=", Pattern.CASE_INSENSITIVE);

    private static final int MAX_INPUT_LENGTH = 10_000;
    private static final int MAX_OUTPUT_LENGTH = 10_000;

    private PromptSanitizer() {}

    /**
     * Sanitize a value for safe injection into an LLM prompt template.
     * Strips Jinja blocks, code fences, HTML/script tags, event handlers,
     * and non-printable control characters. Truncates oversized input.
     */
    public static String sanitizeForPrompt(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        if (input.length() > MAX_INPUT_LENGTH) {
            input = input.substring(0, MAX_INPUT_LENGTH);
        }
        String sanitized = JINJA_VAR.matcher(input).replaceAll("[BLOCKED_TEMPLATE]");
        sanitized = JINJA_STMT.matcher(sanitized).replaceAll("[BLOCKED_TEMPLATE]");
        sanitized = JINJA_VAR_UNCLOSED.matcher(sanitized).replaceAll("[BLOCKED_TEMPLATE]");
        sanitized = JINJA_STMT_UNCLOSED.matcher(sanitized).replaceAll("[BLOCKED_TEMPLATE]");
        sanitized = CODE_FENCE.matcher(sanitized).replaceAll("[BLOCKED_CODE]");
        sanitized = CODE_FENCE_UNCLOSED.matcher(sanitized).replaceAll("[BLOCKED_CODE]");
        sanitized = HTML_COMMENT.matcher(sanitized).replaceAll("");
        sanitized = HTML_TAG.matcher(sanitized).replaceAll("[BLOCKED_HTML]");
        sanitized = EVENT_HANDLER.matcher(sanitized).replaceAll("[BLOCKED_ATTR]=");

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
        return sanitized;
    }

    /**
     * Wrap sanitized user feedback in XML delimiters for clarity in prompts.
     */
    public static String wrapUserFeedback(String input) {
        String sanitized = sanitizeForPrompt(input);
        if (sanitized.isEmpty()) {
            return "";
        }
        return "<user_feedback>\n" + sanitized + "\n</user_feedback>";
    }
}
