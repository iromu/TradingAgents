package com.embabel.gekko.dataflows;

import com.embabel.gekko.util.FileCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * FRED (Federal Reserve Economic Data) API client.
 * Fetches macroeconomic indicators for LLM tool-calling.
 */
@Service
@Slf4j
public class FredService {

    private static final String BASE_URL = "https://api.stlouisfed.org/fred/";
    private static final int DEFAULT_LIMIT = 365;

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final FileCache fileCache;

    public FredService(
            @Value("${app.fred.api-key:}") String apiKey,
            FileCache fileCache
    ) {
        this.apiKey = apiKey;
        this.fileCache = fileCache;
        this.restTemplate = new RestTemplate();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(30000);
        this.restTemplate.setRequestFactory(factory);
    }

    /**
     * Fetch observations for a single FRED series.
     */
    public String getSeries(String seriesId, int limit) {
        if (apiKey == null || apiKey.isBlank()) {
            return "NO_DATA_AVAILABLE: FRED API key not configured (app.fred.api-key)";
        }
        if (seriesId == null || seriesId.isBlank()) {
            return "NO_DATA_AVAILABLE: Series ID is required";
        }

        String cacheKey = "fred:" + seriesId + ":" + limit;
        String cached = fileCache.get(cacheKey, String.class);
        if (cached != null) return cached;

        try {
            String url = UriComponentsBuilder.fromHttpUrl(BASE_URL + "series/observations")
                    .queryParam("series_id", seriesId)
                    .queryParam("api_key", apiKey)
                    .queryParam("file_type", "json")
                    .queryParam("sort_order", "desc")
                    .queryParam("limit", Math.min(limit, 1000))
                    .toUriString();

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null || !"success".equals(response.get("status"))) {
                log.warn("FRED API error for series {}: {}", seriesId, response);
                return "NO_DATA_AVAILABLE: FRED API returned error for series " + seriesId;
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> observations = (List<Map<String, Object>>) response.get("observations");
            if (observations == null || observations.isEmpty()) {
                return "NO_DATA_AVAILABLE: No observations found for series " + seriesId;
            }

            String result = formatAsMarkdown(seriesId, observations);
            fileCache.save(cacheKey, result);
            return result;
        } catch (Exception e) {
            log.error("Failed to fetch FRED series {}: {}", seriesId, e.getMessage());
            return "NO_DATA_AVAILABLE: Failed to fetch FRED data: " + e.getMessage();
        }
    }

    /**
     * Fetch multiple FRED series at once.
     */
    public String getMultipleSeries(List<String> seriesIds) {
        if (apiKey == null || apiKey.isBlank()) {
            return "NO_DATA_AVAILABLE: FRED API key not configured";
        }
        if (seriesIds == null || seriesIds.isEmpty()) {
            return "NO_DATA_AVAILABLE: At least one series ID is required";
        }

        StringBuilder result = new StringBuilder();
        for (String seriesId : seriesIds) {
            result.append("## ").append(seriesId).append("\n\n");
            result.append(getSeries(seriesId, DEFAULT_LIMIT));
            result.append("\n\n");
        }
        return result.toString().trim();
    }

    /**
     * Fetch a standard set of macro indicators.
     */
    public String getDashboard() {
        List<String> defaultSeries = List.of("GDP", "CPIAUCSL", "UNRATE", "FEDFUNDS", "TB3MS");
        return getMultipleSeries(defaultSeries);
    }

    private String formatAsMarkdown(String seriesId, List<Map<String, Object>> observations) {
        StringBuilder sb = new StringBuilder();
        sb.append("| Date | Value | Change | Change Percent |\n");
        sb.append("|------|-------|--------|----------------|\n");

        int count = 0;
        Double prevValue = null;
        for (Map<String, Object> obs : observations) {
            String date = getString(obs, "date", "N/A");
            String valueStr = getString(obs, "value", null);
            if (valueStr == null || valueStr.equals("NA")) continue;

            double value = Double.parseDouble(valueStr);
            String change = "N/A";
            String changePct = "N/A";

            if (prevValue != null && prevValue != 0) {
                double diff = value - prevValue;
                change = String.format("%.2f", diff);
                changePct = String.format("%.2f%%", (diff / prevValue) * 100);
            }

            sb.append("| ").append(date).append(" | ")
                    .append(String.format("%.2f", value)).append(" | ")
                    .append(change).append(" | ")
                    .append(changePct).append(" |\n");

            prevValue = value;
            count++;
            if (count >= 100) break; // Limit output size
        }

        if (count == 0) {
            return "NO_DATA_AVAILABLE: No valid observations for series " + seriesId;
        }
        return sb.toString();
    }

    private String getString(Map<String, Object> map, String key, String defaultValue) {
        Object val = map.get(key);
        return val != null ? val.toString() : defaultValue;
    }
}
