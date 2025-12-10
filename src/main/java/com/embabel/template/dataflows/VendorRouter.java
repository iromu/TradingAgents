package com.embabel.template.dataflows;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VendorRouter {

    private final AlphaVantageService alphaVantageService;

    public String routeToVendor(String method, Object... params) {
        try {
            switch (method) {
                case "get_fundamentals":
                    return alphaVantageService.getFundamentals(
                            (String) params[0],
                            params.length > 1 ? (String) params[1] : null
                    );
                case "get_balance_sheet":
                    return alphaVantageService.getBalanceSheet(
                            (String) params[0],
                            params.length > 1 ? (String) params[1] : "quarterly",
                            params.length > 2 ? (String) params[2] : null
                    );
                case "get_cashflow":
                    return alphaVantageService.getCashflow(
                            (String) params[0],
                            params.length > 1 ? (String) params[1] : "quarterly",
                            params.length > 2 ? (String) params[2] : null
                    );
                case "get_income_statement":
                    return alphaVantageService.getIncomeStatement(
                            (String) params[0],
                            params.length > 1 ? (String) params[1] : "quarterly",
                            params.length > 2 ? (String) params[2] : null
                    );
                default:
                    throw new IllegalArgumentException("Unknown vendor method: " + method);
            }
        } catch (Exception e) {
            return "Error in VendorRouter for method " + method + ": " + e.getMessage();
        }
    }
}
