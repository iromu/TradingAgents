package com.embabel.gekko.dataflows;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Service for querying Alpha Vantage endpoints with a simple file-based cache.
 *
 * <p>This service wraps calls to various Alpha Vantage functions (overview, balance
 * sheet, cash flow, income statement, news & sentiment, insider data) and stores
 * responses in a local cache directory to avoid repeated network calls.</p>
 *
 * <p>Cache files are stored as JSON under the configured {@code cacheDir}. The
 * service uses Spring's {@link RestTemplate} for HTTP requests and expects an
 * Alpha Vantage API key to be provided via configuration property
 * {@code alphavantage.apiKey}.</p>
 * <p>
 * Usage notes:
 * - Public methods return raw JSON responses as {@link String}. The caller is
 * responsible for parsing and validating the JSON payload.
 * - Cache keys are derived from request parameters; several endpoints use a
 * simplified cache key (e.g. {@code TICKER_NEWS}) — be mindful if you need
 * more granular cache invalidation.
 */
@Service
@RequiredArgsConstructor
public class AlphaVantageService {

    /**
     * API key for Alpha Vantage; injected from application configuration.
     */
    @Value("${alphavantage.apiKey}")
    private String apiKey;

    /**
     * Directory used to persist cached API responses. Default: {@code data/alphavantage}.
     * Configurable via {@code alphavantage.cacheDir} property.
     */
    @Value("${alphavantage.cacheDir:data/alphavantage}")
    private String cacheDir;  // configurable cache directory

    private static final String BASE_URL = "https://www.alphavantage.co/query";

    /**
     * RestTemplate instance used to perform HTTP GET requests.
     */
    private final RestTemplate restTemplate = new RestTemplate();

    // -------------------------------
    // Fundamental financial endpoints
    // -------------------------------

    /**
     * Retrieve company fundamentals (OVERVIEW) for the given ticker.
     *
     * @param ticker   The stock ticker symbol (e.g. "AAPL").
     * @param currDate Currently unused — retained for compatibility with callers.
     * @return The raw JSON response from Alpha Vantage as a {@link String}.
     */
    public String getFundamentals(String ticker, String currDate) {
        return getDataWithCache("OVERVIEW", ticker);
    }

    /**
     * Retrieve the company's balance sheet data.
     *
     * @param ticker   The stock ticker symbol.
     * @param freq     Frequency (unused by current implementation but kept for API
     *                 compatibility with potential future changes).
     * @param currDate Currently unused.
     * @return The raw JSON response from Alpha Vantage.
     */
    public String getBalanceSheet(String ticker, String freq, String currDate) {
        return getDataWithCache("BALANCE_SHEET", ticker);
    }

    /**
     * Retrieve the company's cash flow statement.
     *
     * @param ticker   The stock ticker symbol.
     * @param freq     Frequency (unused).
     * @param currDate Currently unused.
     * @return The raw JSON response from Alpha Vantage.
     */
    public String getCashflow(String ticker, String freq, String currDate) {
        return getDataWithCache("CASH_FLOW", ticker);
    }

    /**
     * Retrieve the income statement for the company.
     *
     * @param ticker   The stock ticker symbol.
     * @param freq     Frequency (unused).
     * @param currDate Currently unused.
     * @return The raw JSON response from Alpha Vantage.
     */
    public String getIncomeStatement(String ticker, String freq, String currDate) {
        return getDataWithCache("INCOME_STATEMENT", ticker);
    }
    // =============================================================
    // ============== NEWS & SENTIMENT ==============================
    // =============================================================

    /**
     * Retrieve news and sentiment for a specific ticker between two dates.
     *
     * <p>Note: The cache key is currently simplified to {@code TICKER_NEWS},
     * so subsequent calls with different date ranges may return cached results
     * that do not reflect the requested range. There are commented alternatives
     * in the source showing how to make the cache key date-specific.</p>
     *
     * @param ticker    Stock ticker symbol to fetch news for.
     * @param startDate Start date in ISO format ("yyyy-MM-dd").
     * @param endDate   End date in ISO format ("yyyy-MM-dd").
     * @return Raw JSON response from Alpha Vantage's news_sentiment endpoint.
     */
    public String getNews(String ticker, String startDate, String endDate) {
//        String cacheKey = String.format("%s_NEWS_%s_%s", ticker, startDate, endDate);
        String cacheKey = String.format("%s_NEWS", ticker);
        return getDataWithCache("NEWS_SENTIMENT", cacheKey, builder -> builder
                .queryParam("tickers", ticker)
                .queryParam("time_from", formatDateForApi(startDate))
                .queryParam("time_to", formatDateForApi(endDate))
                .queryParam("limit", "50")
                .queryParam("sort", "LATEST")
        );
    }

    /**
     * Retrieve global news for a topic.
     *
     * @param topic Topic string to filter global news (may be null to fetch general news).
     * @param limit Maximum number of results to return (can be null to use default API behavior).
     * @param page  Page number for paginated results (can be null).
     * @return Raw JSON response from Alpha Vantage.
     */
    public String getGlobalNews(String topic, Integer limit, Integer page) {
//        String cacheKey = String.format("GLOBAL_NEWS_%s_%d_%d", topic, limit, page);
        String cacheKey = "GLOBAL_NEWS";
        return getDataWithCache("NEWS_SENTIMENT", cacheKey, builder -> builder
                .queryParam("topics", topic)
                .queryParam("limit", limit)
                .queryParam("page", page)
                .queryParam("sort", "LATEST")
        );
    }

    /**
     * Retrieve insider sentiment data for a symbol.
     *
     * @param ticker   Stock ticker symbol.
     * @param interval Interval string expected by the API (e.g. "3month").
     * @return Raw JSON response containing insider sentiment data.
     */
    public String getInsiderSentiment(String ticker, String interval) {
//        String cacheKey = String.format("%s_INSIDER_SENTIMENT_%s", ticker, interval);
        String cacheKey = String.format("%s_INSIDER_SENTIMENT", ticker);
        return getDataWithCache("INSIDER_SENTIMENT", cacheKey, builder -> builder
                .queryParam("symbol", ticker)
                .queryParam("interval", interval)
        );
    }

    /**
     * Retrieve insider transactions for the given ticker.
     *
     * @param ticker Stock ticker symbol.
     * @return Raw JSON response containing insider transactions.
     */
    public String getInsiderTransactions(String ticker) {
        String cacheKey = ticker.toUpperCase() + "_INSIDER_TRANSACTIONS";
        return getDataWithCache("INSIDER_TRANSACTIONS", cacheKey, builder -> builder
                .queryParam("symbol", ticker)
        );
    }

    // =============================================================
    // ============== SHARED CACHING LAYER =========================
    // =============================================================

    /**
     * Convenience overload that builds a basic cache key using the symbol and
     * forwards to the generic cache-enabled fetch method.
     *
     * @param function Alpha Vantage function name (e.g. "OVERVIEW").
     * @param symbol   The symbol used to construct the cache key and as a query param.
     * @return Raw JSON response, possibly read from cache.
     */
    private String getDataWithCache(String function, String symbol) {

        String cacheKey = symbol.toUpperCase() + "_" + function;
        return getDataWithCache(function, cacheKey, builder ->
                builder.queryParam("symbol", symbol)
        );
    }

    /**
     * Functional interface allowing callers to customize a {@link UriComponentsBuilder}
     * before the request is executed. Used to add query parameters specific to each
     * endpoint while keeping shared caching logic centralized.
     */
    private interface UrlBuilderCustomizer {
        UriComponentsBuilder customize(UriComponentsBuilder builder);
    }

    /**
     * Centralized method that handles caching logic and performs the HTTP request
     * when cache is missing.
     *
     * <p>Behavior:
     * - Ensures cache directory exists.
     * - Attempts to read cached response from file ({@code cacheDir/cacheKey.json}).
     * - If not cached, builds the request URL, performs the GET, writes the response
     * to the cache, and returns the response.</p>
     *
     * @param function   Alpha Vantage function name.
     * @param cacheKey   Key used to name the cache file (without extension).
     * @param customizer Lambda that adds endpoint-specific query parameters.
     * @return Raw JSON response as a String.
     * @throws RuntimeException if cache directory creation or file IO fails.
     */
    private String getDataWithCache(String function, String cacheKey, UrlBuilderCustomizer customizer) {
        try {
            Files.createDirectories(Paths.get(cacheDir));

            String filename = String.format("%s/%s.json", cacheDir, cacheKey);
            File cacheFile = new File(filename);

            // Read from cache if available
            if (cacheFile.exists() && cacheFile.length() > 0) {
                return Files.readString(cacheFile.toPath());
            }

            // Build query
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(BASE_URL)
                    .queryParam("function", function)
                    .queryParam("apikey", apiKey);

            builder = customizer.customize(builder);

            String url = builder.toUriString();
            String response = restTemplate.getForObject(url, String.class);

            Files.writeString(cacheFile.toPath(), response);

            return response;

        } catch (IOException e) {
            throw new RuntimeException("Cache error: " + e.getMessage(), e);
        }
    }

    /**
     * Convert an ISO date (yyyy-MM-dd) into the timestamp format expected by the
     * Alpha Vantage news_sentiment API (pattern: yyyyMMdd'T'HHmm).
     *
     * @param date ISO date string (e.g. "2023-10-01").
     * @return Formatted date/time string suitable for the API query params.
     */
    private String formatDateForApi(String date) {
        LocalDate parsed = LocalDate.parse(date, DateTimeFormatter.ISO_DATE);
        return parsed.atStartOfDay()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmm"));
    }

}
