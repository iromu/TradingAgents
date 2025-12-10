package com.embabel.template.dataflows;


import com.embabel.template.util.DateUtils;
import com.embabel.template.util.IndicatorMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.VolumeIndicator;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import static com.embabel.template.util.DateUtils.toCalendar;

@Service
@RequiredArgsConstructor
public class YFinService {

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Fetch online historical OHLCV via YahooFinance and return CSV string.
     * Dates format: yyyy-MM-dd
     */
    public String getYFinDataOnline(String symbol, String startDate, String endDate) throws Exception {
        LocalDate start = DateUtils.parseDate(startDate);
        LocalDate end = DateUtils.parseDate(endDate);

        // Convert LocalDate to Calendar
        Calendar startCal = toCalendar(start);
        Calendar endCal = toCalendar(end);

        // YahooFinance Java client (blocking) - acceptable inside a Spring service
        // Fetch stock
        Stock stock = YahooFinance.get(symbol.toUpperCase(), startCal, endCal, Interval.DAILY);
        List<HistoricalQuote> hist = stock.getHistory(startCal, endCal, Interval.DAILY);

        if (hist == null || hist.isEmpty()) {
            return "No data found for symbol '" + symbol + "' between " + startDate + " and " + endDate;
        }

        StringWriter sw = new StringWriter();
        sw.write("# Stock data for " + symbol.toUpperCase() + " from " + startDate + " to " + endDate + "\n");
        sw.write("# Total records: " + hist.size() + "\n");
        sw.write("# Data retrieved on: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n\n");
        sw.write("Date,Open,High,Low,Close,AdjClose,Volume\n");

        ZoneId zone = ZoneId.systemDefault();
        for (HistoricalQuote h : hist) {
            if (h.getDate() == null) continue;
            LocalDate dt = h.getDate().toInstant().atZone(zone).toLocalDate();
            sw.write(DF.format(dt) + ","
                    + toCsvNumber(h.getOpen()) + ","
                    + toCsvNumber(h.getHigh()) + ","
                    + toCsvNumber(h.getLow()) + ","
                    + toCsvNumber(h.getClose()) + ","
                    + toCsvNumber(h.getAdjClose()) + ","
                    + (h.getVolume() == null ? "" : h.getVolume()) + "\n");
        }
        return sw.toString();
    }

    private String toCsvNumber(BigDecimal v) {
        if (v == null) return "";
        return v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /**
     * Returns indicator values for a lookback window like the Python version.
     * indicatorCode must be one of the keys returned by IndicatorMapper.getDescriptions()
     */
    public String getStockStatsIndicatorsWindow(String symbol, String indicatorCode, String currDate, int lookbackDays) throws Exception {
        Map<String, String> desc = IndicatorMapper.getDescriptions();
        if (!desc.containsKey(indicatorCode)) {
            throw new IllegalArgumentException("Indicator " + indicatorCode + " is not supported. Supported keys: " + desc.keySet());
        }

        LocalDate curr = DateUtils.parseDate(currDate);
        LocalDate before = curr.minusDays(lookbackDays);

        BarSeries series = loadBarSeries(symbol, before, curr);
        if (series == null || series.getBarCount() == 0) {
            return "No series data for " + symbol;
        }

        ClosePriceIndicator close = new ClosePriceIndicator(series);
        VolumeIndicator volume = new VolumeIndicator(series);

        org.ta4j.core.Indicator<Num> indicator = IndicatorMapper.buildIndicator(indicatorCode, series, close, volume);

        StringBuilder sb = new StringBuilder();
        sb.append("## ").append(indicatorCode).append(" values from ").append(before.format(DF)).append(" to ").append(curr.format(DF)).append(":\n\n");

        ZoneId zone = ZoneId.systemDefault();
        for (int i = series.getBeginIndex(); i <= series.getEndIndex(); i++) {
            Instant endTimeInstant = series.getBar(i).getEndTime();
            ZonedDateTime endTime = endTimeInstant.atZone(zone);
            LocalDate barDate = endTime.toLocalDate();

            if (barDate.isBefore(before) || barDate.isAfter(curr)) continue;

            Num v = indicator.getValue(i);
            sb.append(barDate.format(DF)).append(": ");
            sb.append(v == null || v.isNaN() ? "N/A" : v.doubleValue());
            sb.append("\n");
        }


        sb.append("\n\n").append(desc.getOrDefault(indicatorCode, "No description available."));
        return sb.toString();
    }

    /**
     * Build a TA4J BarSeries from YahooFinance historical quotes.
     * Uses TA4J 0.18 BasicBar + BarSeriesBuilder (API compatible)
     */

    private BarSeries loadBarSeries(String symbol, LocalDate startInclusive, LocalDate endInclusive) throws Exception {
        Stock stock = YahooFinance.get(symbol.toUpperCase(),
                toCalendar(startInclusive),
                toCalendar(endInclusive),
                Interval.DAILY);
        List<HistoricalQuote> hist = stock.getHistory(toCalendar(startInclusive), toCalendar(endInclusive), Interval.DAILY);

        BarSeries series = new BaseBarSeriesBuilder()
                .withName(symbol.toUpperCase())
                .build();

        ZoneId zone = ZoneId.systemDefault();
        for (HistoricalQuote h : hist) {
            if (h.getDate() == null) continue;
            ZonedDateTime endTime = h.getDate().toInstant().atZone(zone)
                    .withHour(0).withMinute(0).withSecond(0).withNano(0);

            Duration timePeriod = Duration.ofDays(1);
            Instant barEndTime = h.getDate().toInstant(); // renamed from endTime
            Num open = DecimalNum.valueOf(h.getOpen() == null ? Double.NaN : h.getOpen().doubleValue());
            Num high = DecimalNum.valueOf(h.getHigh() == null ? Double.NaN : h.getHigh().doubleValue());
            Num low = DecimalNum.valueOf(h.getLow() == null ? Double.NaN : h.getLow().doubleValue());
            Num close = DecimalNum.valueOf(h.getClose() == null ? Double.NaN : h.getClose().doubleValue());
            Num volume = DecimalNum.valueOf(h.getVolume() == null ? Double.NaN : h.getVolume().doubleValue());

            Num amount = DecimalNum.valueOf(0);
            long trades = 0;

            BaseBar bar = new BaseBar(timePeriod, barEndTime, open, high, low, close, volume, amount, trades);
            series.addBar(bar);

        }

        return series;
    }


}
