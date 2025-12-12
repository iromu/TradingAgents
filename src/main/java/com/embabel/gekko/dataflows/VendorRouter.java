package com.embabel.gekko.dataflows;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class VendorRouter {

    private final AlphaVantageService alphaVantageService;

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
        try {
            return switch (method) {

                // ====================================================
                //                 FUNDAMENTALS
                // ====================================================

                case "get_fundamentals" -> alphaVantageService.getFundamentals(
                        str(params[0], null),
                        str(params.length > 1 ? params[1] : null, null)
                );

                case "get_balance_sheet" -> alphaVantageService.getBalanceSheet(
                        str(params[0], null),
                        str(params.length > 1 ? params[1] : null, "quarterly"),
                        str(params.length > 2 ? params[2] : null, null)
                );

                case "get_cashflow" -> alphaVantageService.getCashflow(
                        str(params[0], null),
                        str(params.length > 1 ? params[1] : null, "quarterly"),
                        str(params.length > 2 ? params[2] : null, null)
                );

                case "get_income_statement" -> alphaVantageService.getIncomeStatement(
                        str(params[0], null),
                        str(params.length > 1 ? params[1] : null, "quarterly"),
                        str(params.length > 2 ? params[2] : null, null)
                );

                // ====================================================
                //              NEWS & SENTIMENT
                // ====================================================

                case "get_news" -> alphaVantageService.getNews(
                        str(params[0], null),
                        str(params.length > 1 ? params[1] : null, "2020-01-01"),
                        str(params.length > 2 ? params[2] : null, "2030-01-01")
                );

                case "get_global_news" -> alphaVantageService.getGlobalNews(
                        str(params[0], "general"),         // topic
                        integer(params.length > 1 ? params[1] : null, 10), // limit
                        integer(params.length > 2 ? params[2] : null, 1)  // page
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

        } catch (Exception e) {
            log.error("Error routing to vendor method: {}", method, e);
            return "Error in VendorRouter for method '" + method + "': " + e.getMessage();
        }
    }
}
