package com.embabel.gekko.tools;


import com.embabel.gekko.dataflows.VendorRouter;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class NewsDataTools {

    private final VendorRouter vendorRouter;

    public NewsDataTools(VendorRouter vendorRouter) {
        this.vendorRouter = vendorRouter;
    }

    @Tool(
            name = "get_news",
            description = "Retrieve news data for a given ticker symbol."
    )
    public String getNews(
            @ToolParam(description = "Ticker symbol") String ticker,
            @ToolParam(description = "Start date yyyy-mm-dd") String startDate,
            @ToolParam(description = "End date yyyy-mm-dd") String endDate
    ) {
        return vendorRouter.routeToVendor("get_news", ticker, startDate, endDate);
    }

    @Tool(
            name = "get_global_news",
            description = "Retrieve global news data."
    )
    public String getGlobalNews(
            @ToolParam(description = "Current date yyyy-mm-dd") String currDate,
            @ToolParam(description = "Days to look back") Integer lookBackDays,
            @ToolParam(description = "Max number of articles") Integer limit
    ) {
        return vendorRouter.routeToVendor("get_global_news", currDate, lookBackDays, limit);
    }

    @Tool(
            name = "get_insider_sentiment",
            description = "Retrieve insider sentiment information about a company."
    )
    public String getInsiderSentiment(
            @ToolParam(description = "Ticker symbol") String ticker,
            @ToolParam(description = "Current date yyyy-mm-dd") String currDate
    ) {
        return vendorRouter.routeToVendor("get_insider_sentiment", ticker, currDate);
    }

    @Tool(
            name = "get_insider_transactions",
            description = "Retrieve insider transaction information about a company."
    )
    public String getInsiderTransactions(
            @ToolParam(description = "Ticker symbol") String ticker,
            @ToolParam(description = "Current date yyyy-mm-dd") String currDate
    ) {
        return vendorRouter.routeToVendor("get_insider_transactions", ticker, currDate);
    }
}
