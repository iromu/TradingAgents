package com.embabel.template.tools;


import com.embabel.template.dataflows.VendorRouter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FundamentalTools {

    private final VendorRouter vendorRouter;

    public FundamentalTools(VendorRouter vendorRouter) {
        this.vendorRouter = vendorRouter;
    }

    @Tool(
            name = "get_fundamentals",
            description = "Retrieve comprehensive fundamental data for a given ticker symbol."
    )
    public String getFundamentals(
            @ToolParam(description = "Ticker symbol") String ticker,
            @ToolParam(description = "Current date yyyy-mm-dd") String currDate
    ) {
        log.info("Getting fundamentals for ticker {}", ticker);
        return vendorRouter.routeToVendor("get_fundamentals", ticker, currDate);
    }

    @Tool(
            name = "get_balance_sheet",
            description = "Retrieve balance sheet data for a given ticker symbol."
    )
    public String getBalanceSheet(
            @ToolParam(description = "Ticker symbol") String ticker,
            @ToolParam(description = "annual/quarterly") String freq,
            @ToolParam(description = "Current date yyyy-mm-dd") String currDate
    ) {
        log.info("Getting balance sheet for ticker {}", ticker);
        return vendorRouter.routeToVendor("get_balance_sheet", ticker, freq, currDate);
    }

    @Tool(
            name = "get_cashflow",
            description = "Retrieve cash flow statement data for a given ticker symbol."
    )
    public String getCashflow(
            @ToolParam(description = "Ticker symbol") String ticker,
            @ToolParam(description = "annual/quarterly") String freq,
            @ToolParam(description = "Current date yyyy-mm-dd") String currDate
    ) {
        log.info("Getting cash flow statement for ticker {}", ticker);
        return vendorRouter.routeToVendor("get_cashflow", ticker, freq, currDate);
    }

    @Tool(
            name = "get_income_statement",
            description = "Retrieve income statement data for a given ticker symbol."
    )
    public String getIncomeStatement(
            @ToolParam(description = "Ticker symbol") String ticker,
            @ToolParam(description = "annual/quarterly") String freq,
            @ToolParam(description = "Current date yyyy-mm-dd") String currDate
    ) {
        log.info("Getting income statement for ticker {}", ticker);
        return vendorRouter.routeToVendor("get_income_statement", ticker, freq, currDate);
    }
}
