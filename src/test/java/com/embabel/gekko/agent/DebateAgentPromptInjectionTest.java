package com.embabel.gekko.agent;

import com.embabel.gekko.domain.ResearchTypes;
import com.embabel.gekko.util.FileCache;
import com.embabel.gekko.util.PromptSanitizer;
import com.embabel.gekko.util.ResultCache;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Tests for prompt sanitization in DebateAgent (now delegated to PromptSanitizer)
 * and extractRating() priority-based extraction.
 */
class DebateAgentPromptInjectionTest {

    private DebateAgent createAgent(Path tempDir) {
        var fileCache = new FileCache(tempDir);
        var resultCache = new ResultCache(fileCache, "5m", "1h");
        return new DebateAgent(
                resultCache, null, null, null, null, null, null, null, null
        );
    }

    // --- PromptSanitizer delegation tests ---

    @Test
    void sanitizeForPrompt_nullInput() {
        assertThat(PromptSanitizer.sanitizeForPrompt(null)).isEmpty();
    }

    @Test
    void sanitizeForPrompt_blankInput() {
        assertThat(PromptSanitizer.sanitizeForPrompt("   ")).isEmpty();
    }

    @Test
    void sanitizeForPrompt_blocksJinjaVariables() {
        String input = "{{ ticker }} is great";
        String result = PromptSanitizer.sanitizeForPrompt(input);
        assertThat(result).doesNotContain("{{").contains("[BLOCKED_TEMPLATE]");
    }

    @Test
    void sanitizeForPrompt_blocksJinjaStatements() {
        String input = "{% if true %}hello{% endif %}";
        String result = PromptSanitizer.sanitizeForPrompt(input);
        assertThat(result).doesNotContain("{%").contains("[BLOCKED_TEMPLATE]");
    }

    @Test
    void sanitizeForPrompt_blocksCodeFences() {
        String input = "```\nmalicious code\n```";
        String result = PromptSanitizer.sanitizeForPrompt(input);
        assertThat(result).doesNotContain("```").contains("[BLOCKED_CODE]");
    }

    @Test
    void sanitizeForPrompt_noXmlWrapper() {
        String input = "Normal text";
        String result = PromptSanitizer.sanitizeForPrompt(input);
        assertThat(result).doesNotContain("<user_feedback>").isEqualTo("Normal text");
    }

    @Test
    void sanitizeForPrompt_normalTextPassthrough() {
        String input = "Debate round 1: Agent A says buy";
        assertThat(PromptSanitizer.sanitizeForPrompt(input)).isEqualTo(input);
    }

    @Test
    void sanitizeForPrompt_stripsControlChars() {
        String input = "hello\u0000world";
        String result = PromptSanitizer.sanitizeForPrompt(input);
        assertThat(result).doesNotContain("\u0000").isEqualTo("helloworld");
    }

    @Test
    void sanitizeForPrompt_truncatesOversized() {
        String input = "a".repeat(12_000);
        String result = PromptSanitizer.sanitizeForPrompt(input);
        assertThat(result.length()).isLessThanOrEqualTo(10_000 + 14);
    }

    // --- buildResearchManagerModel tests ---

    @Test
    void buildResearchManagerModel_includesRiskLevelAndReasoning() {
        var agent = createAgent(Path.of("target/test-cache-risk"));
        var ticker = new ResearchTypes.Ticker("NVDA", "");
        var state = new ResearchTypes.InvestmentDebateState(
                List.of("bull arg", "bear arg"), List.of(), List.of(), "bear", 1, null, 0.0);
        var riskAssessment = new RiskAssessment(RiskLevel.RISKY, "Elevated volatility expected");
        var feedback = new ResearchTypes.InvestmentReviewFeedback("looks good", true);

        @SuppressWarnings("unchecked")
        Map<String, Object> model = (Map<String, Object>) ReflectionTestUtils.invokeMethod(
                agent, "buildResearchManagerModel", ticker, state, riskAssessment, feedback, "Buy NVDA", null);

        assertThat(model.get("risk_level")).isEqualTo("RISKY");
        assertThat(model.get("risk_reasoning")).isEqualTo("Elevated volatility expected");
        assertThat(model.get("ticker")).isEqualTo("NVDA");
        assertThat(model.get("portfolio_decision")).isEqualTo("Buy NVDA");
    }

    @Test
    void buildResearchManagerModel_includesIdentityWhenPresent() {
        var agent = createAgent(Path.of("target/test-cache-identity"));
        var ticker = new ResearchTypes.Ticker("AAPL", "");
        var state = new ResearchTypes.InvestmentDebateState(
                List.of("arg"), List.of(), List.of(), "arg", 1, null, 0.0);
        var identity = new com.embabel.gekko.agent.identity.InstrumentContext(
                "AAPL", "Apple Inc.", "Technology", "Consumer Electronics", "NASDAQ", "USD");

        @SuppressWarnings("unchecked")
        Map<String, Object> model = (Map<String, Object>) ReflectionTestUtils.invokeMethod(
                agent, "buildResearchManagerModel", ticker, state, null, null, "Hold AAPL", identity);

        assertThat(model.get("companyName")).isEqualTo("Apple Inc.");
        assertThat(model.get("sector")).isEqualTo("Technology");
        assertThat(model.get("industry")).isEqualTo("Consumer Electronics");
        assertThat(model.get("exchange")).isEqualTo("NASDAQ");
    }

    @Test
    void buildResearchManagerModel_degradesToPlaceholderOnMissingIdentity() {
        var agent = createAgent(Path.of("target/test-cache-no-identity"));
        var ticker = new ResearchTypes.Ticker("AAPL", "");
        var state = new ResearchTypes.InvestmentDebateState(
                List.of("arg"), List.of(), List.of(), "arg", 1, null, 0.0);

        @SuppressWarnings("unchecked")
        Map<String, Object> model = (Map<String, Object>) ReflectionTestUtils.invokeMethod(
                agent, "buildResearchManagerModel", ticker, state, null, null, "Hold AAPL", null);

        assertThat(model.get("companyName")).isEqualTo("Unknown");
        assertThat(model.get("sector")).isEqualTo("Unknown");
        assertThat(model.get("industry")).isEqualTo("Unknown");
        assertThat(model.get("exchange")).isEqualTo("Unknown");
    }

    @Test
    void buildResearchManagerModel_sanitizesHistory() {
        var agent = createAgent(Path.of("target/test-cache-sanitize-history"));
        var ticker = new ResearchTypes.Ticker("TSLA", "");
        var state = new ResearchTypes.InvestmentDebateState(
                List.of("Bull: {{ malicious }} buy TSLA"), List.of(), List.of(), "buy", 1, null, 0.0);

        @SuppressWarnings("unchecked")
        Map<String, Object> model = (Map<String, Object>) ReflectionTestUtils.invokeMethod(
                agent, "buildResearchManagerModel", ticker, state, null, null, "Buy TSLA", null);

        String history = (String) model.get("history");
        assertThat(history).doesNotContain("{{").contains("[BLOCKED_TEMPLATE]");
    }

    @Test
    void buildResearchManagerModel_wrapsUserFeedbackInXml() {
        var agent = createAgent(Path.of("target/test-cache-feedback"));
        var ticker = new ResearchTypes.Ticker("MSFT", "");
        var state = new ResearchTypes.InvestmentDebateState(
                List.of("arg"), List.of(), List.of(), "arg", 1, null, 0.0);
        var feedback = new ResearchTypes.InvestmentReviewFeedback("I agree with the bull case", true);

        @SuppressWarnings("unchecked")
        Map<String, Object> model = (Map<String, Object>) ReflectionTestUtils.invokeMethod(
                agent, "buildResearchManagerModel", ticker, state, null, feedback, "Buy MSFT", null);

        String userFeedback = (String) model.get("user_feedback");
        assertThat(userFeedback).startsWith("<user_feedback>").endsWith("</user_feedback>");
        assertThat(userFeedback).contains("I agree with the bull case");
    }

    @Test
    void buildResearchManagerModel_userFeedbackNullWhenNotApproved() {
        var agent = createAgent(Path.of("target/test-cache-no-feedback"));
        var ticker = new ResearchTypes.Ticker("MSFT", "");
        var state = new ResearchTypes.InvestmentDebateState(
                List.of("arg"), List.of(), List.of(), "arg", 1, null, 0.0);
        var feedback = new ResearchTypes.InvestmentReviewFeedback(null, false);

        @SuppressWarnings("unchecked")
        Map<String, Object> model = (Map<String, Object>) ReflectionTestUtils.invokeMethod(
                agent, "buildResearchManagerModel", ticker, state, null, feedback, "Hold MSFT", null);

        assertThat(model.get("user_feedback")).isNull();
    }

    // --- extractRating priority tests ---

    @Test
    void extractRating_buyWinsOverOverweight() {
        DebateAgent agent = createAgent(Path.of("target/test-cache-rating-1"));
        String input = "We are overweight this position and recommend a buy.";
        String result = (String) ReflectionTestUtils.invokeMethod(agent, "extractRating", input);
        assertThat(result).isEqualTo("Buy");
    }

    @Test
    void extractRating_sellWinsOverUnderweight() {
        DebateAgent agent = createAgent(Path.of("target/test-cache-rating-2"));
        String input = "Underweight position, time to sell.";
        String result = (String) ReflectionTestUtils.invokeMethod(agent, "extractRating", input);
        assertThat(result).isEqualTo("Sell");
    }

    @Test
    void extractRating_overweightWinsOverHold() {
        DebateAgent agent = createAgent(Path.of("target/test-cache-rating-3"));
        String input = "Hold but overweight relative to benchmark.";
        String result = (String) ReflectionTestUtils.invokeMethod(agent, "extractRating", input);
        assertThat(result).isEqualTo("Overweight");
    }

    @Test
    void extractRating_underweightWinsOverHold() {
        DebateAgent agent = createAgent(Path.of("target/test-cache-rating-4"));
        String input = "Hold but underweight due to risk.";
        String result = (String) ReflectionTestUtils.invokeMethod(agent, "extractRating", input);
        assertThat(result).isEqualTo("Underweight");
    }

    @Test
    void extractRating_buyAlone() {
        DebateAgent agent = createAgent(Path.of("target/test-cache-rating-5"));
        String result = (String) ReflectionTestUtils.invokeMethod(agent, "extractRating", "Strong buy signal.");
        assertThat(result).isEqualTo("Buy");
    }

    @Test
    void extractRating_sellAlone() {
        DebateAgent agent = createAgent(Path.of("target/test-cache-rating-6"));
        String result = (String) ReflectionTestUtils.invokeMethod(agent, "extractRating", "SELL — avoid this stock.");
        assertThat(result).isEqualTo("Sell");
    }

    @Test
    void extractRating_holdDefault() {
        DebateAgent agent = createAgent(Path.of("target/test-cache-rating-7"));
        String result = (String) ReflectionTestUtils.invokeMethod(agent, "extractRating", "No clear signal, mixed fundamentals.");
        assertThat(result).isEqualTo("Hold");
    }

    @Test
    void extractRating_caseInsensitive() {
        DebateAgent agent = createAgent(Path.of("target/test-cache-rating-8"));
        String result = (String) ReflectionTestUtils.invokeMethod(agent, "extractRating", "BUY NOW.");
        assertThat(result).isEqualTo("Buy");
    }

    @Test
    void extractRating_wordBoundaryNoFalsePositive() {
        DebateAgent agent = createAgent(Path.of("target/test-cache-rating-9"));
        String result = (String) ReflectionTestUtils.invokeMethod(agent, "extractRating", "Selling off other positions but holding this one.");
        assertThat(result).isEqualTo("Hold");
    }

    @Test
    void extractRating_emptyInput() {
        DebateAgent agent = createAgent(Path.of("target/test-cache-rating-10"));
        String result = (String) ReflectionTestUtils.invokeMethod(agent, "extractRating", "");
        assertThat(result).isEqualTo("Hold");
    }

    @Test
    void extractRating_buyAndSellConflict_strongBuyWins() {
        DebateAgent agent = createAgent(Path.of("target/test-cache-rating-11"));
        String input = "We recommend a strong buy despite some sell pressure from competitors.";
        String result = (String) ReflectionTestUtils.invokeMethod(agent, "extractRating", input);
        assertThat(result).isEqualTo("Buy");
    }

    @Test
    void extractRating_buyAndSellConflict_strongSellWins() {
        DebateAgent agent = createAgent(Path.of("target/test-cache-rating-12"));
        String input = "Despite early buy interest, we see a strong sell signal forming.";
        String result = (String) ReflectionTestUtils.invokeMethod(agent, "extractRating", input);
        assertThat(result).isEqualTo("Sell");
    }

    @Test
    void extractRating_buyAndSellConflict_defaultsToBuy() {
        DebateAgent agent = createAgent(Path.of("target/test-cache-rating-13"));
        String input = "Some analysts want to buy while others want to sell.";
        String result = (String) ReflectionTestUtils.invokeMethod(agent, "extractRating", input);
        assertThat(result).isEqualTo("Buy");
    }
}
