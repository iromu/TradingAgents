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

    private String getDataWithCache(String function, String symbol) {
        try {
            // Create cache directory if it doesn't exist
            Files.createDirectories(Paths.get(cacheDir));

            String fileName = String.format("%s/%s_%s.json", cacheDir, symbol.toUpperCase(), function);
            File cacheFile = new File(fileName);

            // If cached file exists, read from it
            if (cacheFile.exists() && cacheFile.length() > 0) {
                return Files.readString(cacheFile.toPath());
            }

            // Otherwise call API
            String url = UriComponentsBuilder.fromHttpUrl(BASE_URL)
                    .queryParam("function", function)
                    .queryParam("symbol", symbol)
                    .queryParam("apikey", apiKey)
                    .toUriString();

            String response = restTemplate.getForObject(url, String.class);

            // Save response to cache
            Files.writeString(cacheFile.toPath(), response);

            return response;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read/write cache file", e);
        }
    }
}
