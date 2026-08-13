package com.embabel.gekko.dataflows;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.embabel.gekko.util.AgentUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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

    @Value("${app.alphavantage.output-directory:data/alphavantage}")
    private String cacheDir;

    @Value("${app.alphavantage.connect-timeout-ms:10000}")
    private int connectTimeoutMs;

    @Value("${app.alphavantage.read-timeout-ms:30000}")
    private int readTimeoutMs;

    private static final String BASE_URL = "https://www.alphavantage.co/query";
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final String TICKER_PATTERN = "^[A-Z]{1,6}(\\.[A-Z]{1,2})?$";

    private RestTemplate restTemplate;

    public AlphaVantageService() {
    }

    @PostConstruct
    public void init() {
        int connTimeout = connectTimeoutMs > 0 ? connectTimeoutMs : 10000;
        int readTimeout = readTimeoutMs > 0 ? readTimeoutMs : 30000;

        // RestTemplate with timeouts.
        // API key is appended to URLs in getDataWithCache() — Spring Boot does not log
        // full request URLs by default (only at TRACE wire level), so the key is not
        // exposed in standard application logs.
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
        return getDataWithCache("OVERVIEW", ticker);
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

    public String getBalanceSheet(String ticker, String freq, String currDate) {
        validateTicker(ticker);
        String cacheKey = ticker.toUpperCase() + "_BALANCE_SHEET_" + (freq != null ? freq : "annual");
        return getDataWithCache("BALANCE_SHEET", cacheKey, builder ->
                builder.queryParam("symbol", ticker)
                        .queryParam("frequency", freq != null ? freq : "annual")
        );
    }

    public String getCashflow(String ticker, String freq, String currDate) {
        validateTicker(ticker);
        String cacheKey = ticker.toUpperCase() + "_CASH_FLOW_" + (freq != null ? freq : "annual");
        return getDataWithCache("CASH_FLOW", cacheKey, builder ->
                builder.queryParam("symbol", ticker)
                        .queryParam("frequency", freq != null ? freq : "annual")
        );
    }

    public String getIncomeStatement(String ticker, String freq, String currDate) {
        validateTicker(ticker);
        String cacheKey = ticker.toUpperCase() + "_INCOME_STATEMENT_" + (freq != null ? freq : "annual");
        return getDataWithCache("INCOME_STATEMENT", cacheKey, builder ->
                builder.queryParam("symbol", ticker)
                        .queryParam("frequency", freq != null ? freq : "annual")
        );
    }

    // --- News & sentiment endpoints ---

    public String getNews(String ticker, String startDate, String endDate) {
        validateTicker(ticker);
        String cacheKey = String.format("%s_NEWS_%s_%s", ticker, startDate, endDate);
        return getDataWithCache("NEWS_SENTIMENT", cacheKey, builder -> builder
                .queryParam("tickers", ticker)
                .queryParam("time_from", formatDateForApi(startDate))
                .queryParam("time_to", formatDateForApi(endDate))
                .queryParam("limit", "50")
                .queryParam("sort", "LATEST")
        );
    }

    public String getGlobalNews(String topic, Integer limit, Integer page) {
        String topicKey = topic != null ? topic : "null";
        String limitKey = limit != null ? String.valueOf(limit) : "null";
        String pageKey = page != null ? String.valueOf(page) : "null";
        String cacheKey = String.format("GLOBAL_NEWS_%s_%s_%s", topicKey, limitKey, pageKey);
        return getDataWithCache("NEWS_SENTIMENT", cacheKey, builder -> {
            if (topic != null) builder.queryParam("topics", topic);
            if (limit != null) builder.queryParam("limit", limit);
            if (page != null) builder.queryParam("page", page);
            builder.queryParam("sort", "LATEST");
            return builder;
        });
    }

    public String getInsiderSentiment(String ticker, String interval) {
        validateTicker(ticker);
        String cacheKey = String.format("%s_INSIDER_SENTIMENT_%s", ticker, interval);
        return getDataWithCache("INSIDER_SENTIMENT", cacheKey, builder -> builder
                .queryParam("symbol", ticker)
                .queryParam("interval", interval)
        );
    }

    public String getInsiderTransactions(String ticker) {
        validateTicker(ticker);
        String cacheKey = ticker.toUpperCase() + "_INSIDER_TRANSACTIONS";
        return getDataWithCache("INSIDER_TRANSACTIONS", cacheKey, builder -> builder
                .queryParam("symbol", ticker)
        );
    }

    // --- Shared caching layer ---

    private String getDataWithCache(String function, String symbol) {
        String cacheKey = symbol.toUpperCase() + "_" + function;
        return getDataWithCache(function, cacheKey, builder ->
                builder.queryParam("symbol", symbol)
        );
    }

    private interface UrlBuilderCustomizer {
        UriComponentsBuilder customize(UriComponentsBuilder builder);
    }

    private String getDataWithCache(String function, String cacheKey, UrlBuilderCustomizer customizer) {
        try {
            Files.createDirectories(Paths.get(cacheDir));

            String filename = String.format("%s/%s.json", cacheDir, cacheKey);
            File cacheFile = new File(filename);

            // Read from cache if available
            if (cacheFile.exists() && cacheFile.length() > 0) {
                return Files.readString(cacheFile.toPath());
            }

            // Build query URL with function and apikey
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(BASE_URL)
                    .queryParam("function", function)
                    .queryParam("apikey", apiKey);

            builder = customizer.customize(builder);

            String url = builder.toUriString();
            String response = restTemplate.getForObject(url, String.class);

            if (response != null) {
                Path cachePath = cacheFile.toPath();
                Path tempPath = cachePath.resolveSibling(cachePath.getFileName() + ".tmp");
                try {
                    Files.writeString(tempPath, response);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to write cache for key " + cacheKey + ": " + e.getMessage(), e);
                }
                try {
                    Files.move(tempPath, cachePath, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    log.error("Failed to atomically save cache for key {}: {}", cacheKey, e.getMessage());
                }
            }

            return response;

        } catch (IOException e) {
            throw new RuntimeException("Cache error: " + e.getMessage(), e);
        }
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
