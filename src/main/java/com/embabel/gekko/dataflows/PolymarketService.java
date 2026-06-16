package com.embabel.gekko.dataflows;

import com.embabel.gekko.util.FileCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Polymarket prediction market API client.
 * No API key required.
 */
@Service
@Slf4j
public class PolymarketService {

    private static final String BASE_URL = "https://clob.polymarket.com/";

    private final RestTemplate restTemplate;
    private final FileCache fileCache;

    public PolymarketService(FileCache fileCache) {
        this.fileCache = fileCache;
        this.restTemplate = new RestTemplate();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(30000);
        this.restTemplate.setRequestFactory(factory);
    }

    /**
     * Search for prediction markets by topic.
     */
    public String searchMarkets(String query) {
        if (query == null || query.isBlank()) {
            return "NO_DATA_AVAILABLE: Search query is required";
        }

        String cacheKey = "polymarket:search:" + query.toLowerCase();
        String cached = fileCache.get(cacheKey, String.class);
        if (cached != null) return cached;

        try {
            String url = UriComponentsBuilder.fromHttpUrl(BASE_URL + "markets")
                    .queryParam("search", query)
                    .queryParam("limit", 20)
                    .toUriString();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> markets = restTemplate.getForObject(url, List.class);

            if (markets == null || markets.isEmpty()) {
                return "NO_DATA_AVAILABLE: No markets found for query: " + query;
            }

            String result = formatAsMarkdown(markets);
            fileCache.save(cacheKey, result);
            return result;
        } catch (Exception e) {
            log.error("Failed to search Polymarket markets for '{}': {}", query, e.getMessage());
            return "NO_DATA_AVAILABLE: Failed to fetch Polymarket data: " + e.getMessage();
        }
    }

    /**
     * Get a specific market by slug.
     */
    public String getMarket(String slug) {
        if (slug == null || slug.isBlank()) {
            return "NO_DATA_AVAILABLE: Market slug is required";
        }

        String cacheKey = "polymarket:market:" + slug.toLowerCase();
        String cached = fileCache.get(cacheKey, String.class);
        if (cached != null) return cached;

        try {
            String url = UriComponentsBuilder.fromHttpUrl(BASE_URL + "market")
                    .queryParam("slug", slug)
                    .toUriString();

            @SuppressWarnings("unchecked")
            Map<String, Object> market = restTemplate.getForObject(url, Map.class);

            if (market == null) {
                return "NO_DATA_AVAILABLE: Market not found: " + slug;
            }

            String result = formatMarketDetail(slug, market);
            fileCache.save(cacheKey, result);
            return result;
        } catch (Exception e) {
            log.error("Failed to fetch Polymarket market '{}': {}", slug, e.getMessage());
            return "NO_DATA_AVAILABLE: Failed to fetch Polymarket data: " + e.getMessage();
        }
    }

    private String formatAsMarkdown(List<Map<String, Object>> markets) {
        StringBuilder sb = new StringBuilder();
        sb.append("| Market | Outcome | Probability |\n");
        sb.append("|--------|---------|-------------|\n");

        for (Map<String, Object> market : markets) {
            String title = getString(market, "question", getString(market, "title", "N/A"));
            String slug = getString(market, "slug", "N/A");
            // Polymarket API returns outcomes as a list or a single outcome field
            Object outcome = market.get("outcome");
            if (outcome == null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> outcomes = (List<Map<String, Object>>) market.get("outcomes");
                if (outcomes != null && !outcomes.isEmpty()) {
                    outcome = outcomes.get(0).get("name");
                }
            }
            String outcomeStr = outcome != null ? outcome.toString() : "N/A";
            String prob = getString(market, "probability", getString(market, "price", outcomeStr));

            sb.append("| [").append(title).append("](").append("https://polymarket.com/mark/").append(slug).append(") | ")
                    .append(outcomeStr).append(" | ")
                    .append(prob).append(" |\n");
        }
        return sb.toString();
    }

    private String formatMarketDetail(String slug, Map<String, Object> market) {
        StringBuilder sb = new StringBuilder();
        sb.append("## ").append(getString(market, "question", slug)).append("\n\n");
        sb.append("- **Slug**: ").append(slug).append("\n");
        sb.append("- **Status**: ").append(getString(market, "closed", "false")).append("\n");
        sb.append("- **Volume**: ").append(getString(market, "volume", "N/A")).append("\n");
        sb.append("\n**Outcomes**:\n");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> outcomes = (List<Map<String, Object>>) market.get("outcomes");
        if (outcomes != null) {
            for (Map<String, Object> outcome : outcomes) {
                sb.append("- ").append(getString(outcome, "name", "N/A"))
                        .append(": ").append(getString(outcome, "price", "N/A")).append("\n");
            }
        }
        return sb.toString();
    }

    private String getString(Map<String, Object> map, String key, String defaultValue) {
        Object val = map.get(key);
        return val != null ? val.toString() : defaultValue;
    }
}
