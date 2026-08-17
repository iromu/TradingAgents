package com.embabel.gekko.dataflows;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.embabel.gekko.util.AgentUtils;
import com.embabel.gekko.util.ResultCache;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for querying Alpha Vantage endpoints with file-based caching.
 * Cache files stored as JSON under {@code cacheDir}.
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "app.alphavantage", name = "enabled", havingValue = "true", matchIfMissing = false)
public class AlphaVantageService {

    @Value("${app.alphavantage.api-key:dummy_key}")
    private String apiKey;

    private final ResultCache resultCache;

    @Value("${app.alphavantage.connect-timeout-ms:10000}")
    private int connectTimeoutMs;

    @Value("${app.alphavantage.read-timeout-ms:30000}")
    private int readTimeoutMs;

    private static final String BASE_URL = "https://www.alphavantage.co/query";
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final String TICKER_PATTERN = "^[A-Z]{1,6}(\\.[A-Z]{1,2})?$";

    private RestTemplate restTemplate;

    public AlphaVantageService(ResultCache resultCache) {
        this.resultCache = resultCache;
    }

    @PostConstruct
    public void init() {
        int connTimeout = connectTimeoutMs > 0 ? connectTimeoutMs : 10000;
        int readTimeout = readTimeoutMs > 0 ? readTimeoutMs : 30000;
        this.restTemplate = AgentUtils.restTemplate(connTimeout, readTimeout);
    }

    // --- Ticker validation ---

    /**
     * Validate a ticker symbol.
     * Accepts 1-6 uppercase letters, optionally followed by a dot and 1-2 uppercase letters
     * (e.g. AAPL, GOOGL, BRK.B).
     *
     * @param ticker the ticker symbol to validate
     * @throws IllegalArgumentException if the ticker is invalid
     */
    public void validateTicker(String ticker) {
        if (ticker == null || !ticker.matches(TICKER_PATTERN)) {
            throw new IllegalArgumentException(
                    "Invalid ticker symbol: '" + ticker + "'. Expected format: 1-6 uppercase letters, optionally followed by .XX (e.g. AAPL, BRK.B)");
        }
    }

    // --- Fundamental financial endpoints ---

    public String getFundamentals(String ticker) {
        validateTicker(ticker);
        String cacheKey = ResultCache.canonicalKey(ResultCache.CATEGORY_EXTERNAL_HTTP, "alphavantage", "OVERVIEW", ticker);
        return resultCache.getOrCompute(ResultCache.CATEGORY_EXTERNAL_HTTP, cacheKey, String.class,
                () -> fetchData("OVERVIEW", builder -> builder.queryParam("symbol", ticker)));
    }

    private static final String[] OVERVIEW_FIELDS = {"Name", "Sector", "Industry", "Exchange", "Currency", "Symbol"};

    public Map<String, String> getOverview(String ticker) {
        validateTicker(ticker);
        String json = getFundamentals(ticker);
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            JsonNode node = JSON_MAPPER.readTree(json);
            if (node == null || node.isMissingNode()) {
                return null;
            }
            Map<String, String> map = new LinkedHashMap<>();
            for (String field : OVERVIEW_FIELDS) {
                JsonNode val = node.get(field);
                map.put(field, val != null && !val.isNull() ? val.asText() : null);
            }
            return map;
        } catch (Exception e) {
            log.warn("Failed to parse OVERVIEW for {}: {}", ticker, e.getMessage());
            return null;
        }
    }

    public String getBalanceSheet(String ticker, String freq) {
        validateTicker(ticker);
        String frequency = freq != null ? freq : "annual";
        String cacheKey = ResultCache.canonicalKey(ResultCache.CATEGORY_EXTERNAL_HTTP, "alphavantage", "BALANCE_SHEET", ticker, frequency);
        return resultCache.getOrCompute(ResultCache.CATEGORY_EXTERNAL_HTTP, cacheKey, String.class,
                () -> fetchData("BALANCE_SHEET", builder -> builder
                        .queryParam("symbol", ticker)
                        .queryParam("frequency", frequency)));
    }

    public String getCashflow(String ticker, String freq) {
        validateTicker(ticker);
        String frequency = freq != null ? freq : "annual";
        String cacheKey = ResultCache.canonicalKey(ResultCache.CATEGORY_EXTERNAL_HTTP, "alphavantage", "CASH_FLOW", ticker, frequency);
        return resultCache.getOrCompute(ResultCache.CATEGORY_EXTERNAL_HTTP, cacheKey, String.class,
                () -> fetchData("CASH_FLOW", builder -> builder
                        .queryParam("symbol", ticker)
                        .queryParam("frequency", frequency)));
    }

    public String getIncomeStatement(String ticker, String freq) {
        validateTicker(ticker);
        String frequency = freq != null ? freq : "annual";
        String cacheKey = ResultCache.canonicalKey(ResultCache.CATEGORY_EXTERNAL_HTTP, "alphavantage", "INCOME_STATEMENT", ticker, frequency);
        return resultCache.getOrCompute(ResultCache.CATEGORY_EXTERNAL_HTTP, cacheKey, String.class,
                () -> fetchData("INCOME_STATEMENT", builder -> builder
                        .queryParam("symbol", ticker)
                        .queryParam("frequency", frequency)));
    }

    // --- News & sentiment endpoints ---

    public String getNews(String ticker, String startDate, String endDate) {
        validateTicker(ticker);
        String from = formatDateForApi(startDate);
        String to = formatDateForApi(endDate);
        String cacheKey = ResultCache.canonicalKey(ResultCache.CATEGORY_EXTERNAL_HTTP, "alphavantage", "NEWS_SENTIMENT", ticker, from, to);
        return resultCache.getOrCompute(ResultCache.CATEGORY_EXTERNAL_HTTP, cacheKey, String.class,
                () -> fetchData("NEWS_SENTIMENT", builder -> builder
                        .queryParam("tickers", ticker)
                        .queryParam("time_from", from)
                        .queryParam("time_to", to)
                        .queryParam("limit", "50")
                        .queryParam("sort", "LATEST")));
    }

    public String getGlobalNews(String topic, Integer limit, Integer page) {
        String topicKey = topic != null ? topic : "null";
        String limitKey = limit != null ? String.valueOf(limit) : "null";
        String pageKey = page != null ? String.valueOf(page) : "null";
        String cacheKey = ResultCache.canonicalKey(ResultCache.CATEGORY_EXTERNAL_HTTP, "alphavantage", "GLOBAL_NEWS", topicKey, limitKey, pageKey);
        return resultCache.getOrCompute(ResultCache.CATEGORY_EXTERNAL_HTTP, cacheKey, String.class,
                () -> fetchData("NEWS_SENTIMENT", builder -> {
                    if (topic != null) builder.queryParam("topics", topic);
                    if (limit != null) builder.queryParam("limit", limit);
                    if (page != null) builder.queryParam("page", page);
                    builder.queryParam("sort", "LATEST");
                    return builder;
                }));
    }

    public String getInsiderSentiment(String ticker, String interval) {
        validateTicker(ticker);
        String cacheKey = ResultCache.canonicalKey(ResultCache.CATEGORY_EXTERNAL_HTTP, "alphavantage", "INSIDER_SENTIMENT", ticker, interval);
        return resultCache.getOrCompute(ResultCache.CATEGORY_EXTERNAL_HTTP, cacheKey, String.class,
                () -> fetchData("INSIDER_SENTIMENT", builder -> builder
                        .queryParam("symbol", ticker)
                        .queryParam("interval", interval)));
    }

    public String getInsiderTransactions(String ticker) {
        validateTicker(ticker);
        String cacheKey = ResultCache.canonicalKey(ResultCache.CATEGORY_EXTERNAL_HTTP, "alphavantage", "INSIDER_TRANSACTIONS", ticker);
        return resultCache.getOrCompute(ResultCache.CATEGORY_EXTERNAL_HTTP, cacheKey, String.class,
                () -> fetchData("INSIDER_TRANSACTIONS", builder -> builder.queryParam("symbol", ticker)));
    }

    // --- Shared fetch layer ---

    private interface UrlBuilderCustomizer {
        UriComponentsBuilder customize(UriComponentsBuilder builder);
    }

    private String fetchData(String function, UrlBuilderCustomizer customizer) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(BASE_URL)
                .queryParam("function", function)
                .queryParam("apikey", apiKey);
        builder = customizer.customize(builder);
        String url = builder.toUriString();
        return restTemplate.getForObject(url, String.class);
    }

    /**
     * Format a date string for the Alpha Vantage API.
     * Accepts ISO-8601 dates (yyyy-MM-dd) and converts to yyyyMMdd'T'HHmm format.
     * On parse failure (null, empty, malformed input from LLM), returns the current
     * date formatted in the expected API format rather than crashing the agent turn.
     *
     * @param date the date string to format, or null
     * @return formatted date string for the API
     */
    String formatDateForApi(String date) {
        try {
            if (date == null || date.isBlank()) {
                log.warn("Null or blank date provided to formatDateForApi, using current date");
                return formatLocalDate(LocalDate.now());
            }
            LocalDate parsed = LocalDate.parse(date, DateTimeFormatter.ISO_DATE);
            return formatLocalDate(parsed);
        } catch (Exception e) {
            log.warn("Failed to parse date '{}' for Alpha Vantage API, using current date. Error: {}",
                    date, e.getMessage());
            return formatLocalDate(LocalDate.now());
        }
    }

    private String formatLocalDate(LocalDate date) {
        return date.atStartOfDay()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmm"));
    }

}
