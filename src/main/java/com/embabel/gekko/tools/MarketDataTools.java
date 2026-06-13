package com.embabel.gekko.tools;

import com.embabel.gekko.dataflows.YFinService;
import com.embabel.gekko.util.DateUtils;
import com.embabel.gekko.util.IndicatorMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.VolumeIndicator;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
public class MarketDataTools {

    private final YFinService yFinService;

    public MarketDataTools(YFinService yFinService) {
        this.yFinService = yFinService;
    }

    @Tool(
            name = "get_stock_data",
            description = "Get historical stock price data (OHLCV) for a ticker symbol."
    )
    public String get_stock_data(
            @ToolParam(description = "Ticker symbol (e.g. AAPL)") String ticker,
            @ToolParam(description = "Start date in yyyy-MM-dd format") String startDate,
            @ToolParam(description = "End date in yyyy-MM-dd format") String endDate
    ) {
        log.info("Getting stock data for ticker {} from {} to {}", ticker, startDate, endDate);
        try {
            return yFinService.getYFinDataOnline(ticker, startDate, endDate);
        } catch (Exception e) {
            return "Error fetching stock data for " + ticker + ": " + e.getMessage();
        }
    }

    @Tool(
            name = "get_indicators",
            description = "Calculate technical indicators for a ticker using TA4J."
    )
    public String get_indicators(
            @ToolParam(description = "Ticker symbol (e.g. AAPL)") String ticker,
            @ToolParam(description = "Comma-separated list of indicator codes (e.g. SMA,RSI,MACD)") String indicators,
            @ToolParam(description = "Current date in yyyy-MM-dd format") String currDate,
            @ToolParam(description = "Lookback period in days") int lookbackDays
    ) {
        log.info("Getting indicators {} for ticker {} with lookback {} days", indicators, ticker, lookbackDays);
        try {
            String[] indicatorCodes = indicators.split(",");
            StringBuilder result = new StringBuilder();
            for (String code : indicatorCodes) {
                String trimmed = code.trim();
                if (trimmed.isEmpty()) continue;
                try {
                    String indicatorResult = yFinService.getStockStatsIndicatorsWindow(
                            ticker, trimmed, currDate, lookbackDays);
                    result.append(indicatorResult).append("\n\n");
                } catch (Exception e) {
                    result.append("Error calculating ").append(trimmed).append(": ").append(e.getMessage()).append("\n\n");
                }
            }
            return result.toString();
        } catch (Exception e) {
            return "Error fetching indicators for " + ticker + ": " + e.getMessage();
        }
    }
}
