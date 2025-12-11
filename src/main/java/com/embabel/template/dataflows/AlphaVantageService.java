package com.embabel.template.dataflows;

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

@Service
@RequiredArgsConstructor
public class AlphaVantageService {

    @Value("${alphavantage.apiKey}")
    private String apiKey;

    @Value("${alphavantage.cacheDir:data/alphavantage}")
    private String cacheDir;  // configurable cache directory

    private static final String BASE_URL = "https://www.alphavantage.co/query";
    private final RestTemplate restTemplate = new RestTemplate();

    public String getFundamentals(String ticker, String currDate) {
        return getDataWithCache("OVERVIEW", ticker);
    }

    public String getBalanceSheet(String ticker, String freq, String currDate) {
        return getDataWithCache("BALANCE_SHEET", ticker);
    }

    public String getCashflow(String ticker, String freq, String currDate) {
        return getDataWithCache("CASH_FLOW", ticker);
    }

    public String getIncomeStatement(String ticker, String freq, String currDate) {
        return getDataWithCache("INCOME_STATEMENT", ticker);
    }
    // =============================================================
    // ============== NEWS & SENTIMENT ==============================
    // =============================================================

    public String getNews(String ticker, String startDate, String endDate) {
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
        String cacheKey = String.format("GLOBAL_NEWS_%s_%d_%d", topic, limit, page);
        return getDataWithCache("NEWS_SENTIMENT", cacheKey, builder -> builder
                .queryParam("topics", topic)
                .queryParam("limit", limit)
                .queryParam("page", page)
                .queryParam("sort", "LATEST")
        );
    }

    public String getInsiderSentiment(String ticker, String interval) {
        String cacheKey = String.format("%s_INSIDER_SENTIMENT_%s", ticker, interval);
        return getDataWithCache("INSIDER_SENTIMENT", cacheKey, builder -> builder
                .queryParam("symbol", ticker)
                .queryParam("interval", interval)
        );
    }

    public String getInsiderTransactions(String ticker) {
        String cacheKey = ticker.toUpperCase() + "_INSIDER_TRANSACTIONS";
        return getDataWithCache("INSIDER_TRANSACTIONS", cacheKey, builder -> builder
                .queryParam("symbol", ticker)
        );
    }

    // =============================================================
    // ============== SHARED CACHING LAYER =========================
    // =============================================================
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

    private String formatDateForApi(String date) {
        LocalDate parsed = LocalDate.parse(date, DateTimeFormatter.ISO_DATE);
        return parsed.atStartOfDay()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmm"));
    }

}
