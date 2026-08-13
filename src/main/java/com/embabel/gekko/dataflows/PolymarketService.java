package com.embabel.gekko.dataflows;

import com.embabel.gekko.util.AgentUtils;
import com.embabel.gekko.util.FileCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

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
        this.restTemplate = AgentUtils.restTemplate(10000, 30000);
    }

    /**
     * Search for prediction markets by topic.
     */
    public String searchMarkets(String query) {
        if (query == null || query.isBlank()) {
            return "NO_DATA_AVAILABLE: Search query is required";
        }

        String cacheKey = "polymarket:search:" + query.toLowerCase();
        return fileCache.getOrCompute(cacheKey, String.class, () -> fetchSearch(query));
    }

    private String fetchSearch(String query) {
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

            return formatAsMarkdown(markets);
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

        String normalized = slug.toLowerCase();
        String cacheKey = "polymarket:market:" + normalized;
        return fileCache.getOrCompute(cacheKey, String.class, () -> fetchMarket(normalized));
    }

    private String fetchMarket(String slug) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(BASE_URL + "market")
                    .queryParam("slug", slug)
                    .toUriString();

            @SuppressWarnings("unchecked")
            Map<String, Object> market = restTemplate.getForObject(url, Map.class);

            if (market == null) {
                return "NO_DATA_AVAILABLE: Market not found: " + slug;
            }

            return formatMarketDetail(slug, market);
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
            String title = AgentUtils.mapString(market, "question", AgentUtils.mapString(market, "title", "N/A"));
            String slug = AgentUtils.mapString(market, "slug", "N/A");
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
            String prob = AgentUtils.mapString(market, "probability",
                    AgentUtils.mapString(market, "price", "N/A"));

            sb.append("| [").append(title).append("](").append("https://polymarket.com/mark/").append(slug).append(") | ")
                    .append(outcomeStr).append(" | ")
                    .append(prob).append(" |\n");
        }
        return sb.toString();
    }

    private String formatMarketDetail(String slug, Map<String, Object> market) {
        StringBuilder sb = new StringBuilder();
        sb.append("## ").append(AgentUtils.mapString(market, "question", slug)).append("\n\n");
        sb.append("- **Slug**: ").append(slug).append("\n");
        sb.append("- **Status**: ").append(AgentUtils.mapString(market, "closed", "false")).append("\n");
        sb.append("- **Volume**: ").append(AgentUtils.mapString(market, "volume", "N/A")).append("\n");
        sb.append("\n**Outcomes**:\n");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> outcomes = (List<Map<String, Object>>) market.get("outcomes");
        if (outcomes != null) {
            for (Map<String, Object> outcome : outcomes) {
                sb.append("- ").append(AgentUtils.mapString(outcome, "name", "N/A"))
                        .append(": ").append(AgentUtils.mapString(outcome, "price", "N/A")).append("\n");
            }
        }
        return sb.toString();
    }
}
