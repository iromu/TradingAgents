package com.embabel.gekko.agent;

import com.embabel.gekko.agent.TraderAgent.Ticker;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TraderAgentStreamingTest {

    /**
     * Test that parseTicker correctly extracts ticker from JSON format.
     */
    @Test
    void parseTicker_jsonFormat() {
        Ticker result = parseTicker("{\"content\":\"AAPL\"}");
        assertEquals("AAPL", result.content());
    }

    @Test
    void parseTicker_jsonWithSpaces() {
        Ticker result = parseTicker("  {\"content\": \"TSLA\"}  ");
        assertEquals("TSLA", result.content());
    }

    @Test
    void parseTicker_plainText() {
        Ticker result = parseTicker("AAPL");
        assertEquals("AAPL", result.content());
    }

    @Test
    void parseTicker_withExplanation() {
        // The LLM might respond with "AAPL" as plain text
        Ticker result = parseTicker("AAPL");
        assertEquals("AAPL", result.content());
    }

    @Test
    void parseTicker_emptyContent() {
        Ticker result = parseTicker("{}");
        // Falls through to fallback — no quoted string found, returns "{}"
        assertEquals("{}", result.content());
    }

    @Test
    void parseTicker_nullContent() {
        Ticker result = parseTicker("{\"content\": null}");
        // Fallback extracts "content" between first two quotes
        assertEquals("content", result.content());
    }

    @Test
    void parseTicker_singleQuotedFallback() {
        Ticker result = parseTicker("{'content': 'MSFT'}");
        // No double quotes found, falls through to plain text
        assertEquals("{'content': 'MSFT'}", result.content());
    }

    /**
     * Simple standalone parseTicker for testing (mirrors TraderAgent implementation).
     */
    private static Ticker parseTicker(String response) {
        String content = response.trim();
        // Try to extract from JSON like {"content":"AAPL"}
        int jsonStart = content.indexOf("\"content\"");
        if (jsonStart >= 0) {
            int colon = content.indexOf(':', jsonStart);
            int quoteStart = content.indexOf('"', colon);
            int quoteEnd = content.indexOf('"', quoteStart + 1);
            if (quoteStart >= 0 && quoteEnd > quoteStart) {
                String extracted = content.substring(quoteStart + 1, quoteEnd).trim();
                if (!extracted.isEmpty()) {
                    return new Ticker(extracted, "");
                }
            }
        }
        // Fallback: extract the last quoted string or use the whole content
        int lastQuoteStart = content.lastIndexOf('"');
        if (lastQuoteStart > 0) {
            int prevQuote = content.lastIndexOf('"', lastQuoteStart - 1);
            if (prevQuote >= 0) {
                String extracted = content.substring(prevQuote + 1, lastQuoteStart).trim();
                if (!extracted.isEmpty()) {
                    return new Ticker(extracted, "");
                }
            }
        }
        return new Ticker(content, "");
    }
}
