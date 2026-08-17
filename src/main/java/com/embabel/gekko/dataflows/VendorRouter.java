package com.embabel.gekko.dataflows;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class VendorRouter {

    @Autowired(required = false)
    private AlphaVantageService alphaVantageService;

    /**
     * Set the Alpha Vantage service (for testing).
     */
    public void setAlphaVantageService(AlphaVantageService alphaVantageService) {
        this.alphaVantageService = alphaVantageService;
    }

    // --------------------------
    // Helpers for safe extraction
    // --------------------------
    private String str(Object o, String def) {
        return (o instanceof String s && !s.isBlank()) ? s : def;
    }

    private Integer integer(Object o, Integer def) {
        return (o instanceof Integer i) ? i : def;
    }

    // --------------------------
    // Main router method
    // --------------------------
    public String routeToVendor(String method, Object... params) {
        if (alphaVantageService == null) {
            return "Alpha Vantage service not configured — set app.alphavantage.enabled=true and provide app.alphavantage.api-key";
        }
        if (params == null || params.length == 0) {
            return "ERROR: No parameters provided for method: " + method;
        }
        return switch (method) {

            // ====================================================
            //                 FUNDAMENTALS
            // ====================================================

            case "get_fundamentals" -> alphaVantageService.getFundamentals(
                    str(params[0], null)
            );

            case "get_balance_sheet" -> alphaVantageService.getBalanceSheet(
                    str(params[0], null),
                    str(params.length > 1 ? params[1] : null, "quarterly")
            );

            case "get_cashflow" -> alphaVantageService.getCashflow(
                    str(params[0], null),
                    str(params.length > 1 ? params[1] : null, "quarterly")
            );

            case "get_income_statement" -> alphaVantageService.getIncomeStatement(
                    str(params[0], null),
                    str(params.length > 1 ? params[1] : null, "quarterly")
            );

            // ====================================================
            //              NEWS & SENTIMENT
            // ====================================================

            case "get_news" -> alphaVantageService.getNews(
                    str(params[0], null),
                    str(params.length > 1 ? params[1] : null, "2020-01-01"),
                    str(params.length > 2 ? params[2] : null, "2030-01-01")
            );

            // NewsDataTools.getGlobalNews(currDate, lookBackDays, limit)
            // Alpha Vantage NEWS_SENTIMENT has no date params; currDate/lookBackDays
            // are discarded, and the caller's limit becomes the API limit.
            case "get_global_news" -> alphaVantageService.getGlobalNews(
                    null,                              // topic (null = no filter)
                    integer(params.length > 2 ? params[2] : null, 10), // limit (from caller's limit param)
                    integer(params.length > 3 ? params[3] : null, 1)   // page (optional, defaults to 1)
            );

            // ====================================================
            //                INSIDER DATA
            // ====================================================

            case "get_insider_sentiment" -> alphaVantageService.getInsiderSentiment(
                    str(params[0], null),
                    str(params.length > 1 ? params[1] : null, "12M")
            );

            case "get_insider_transactions" -> alphaVantageService.getInsiderTransactions(
                    str(params[0], null)
            );

            default -> throw new IllegalArgumentException("Unknown vendor method: " + method);
        };
    }
}
