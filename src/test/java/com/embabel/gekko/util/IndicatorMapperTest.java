package com.embabel.gekko.util;

import org.junit.jupiter.api.Test;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.ATRIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.averages.EMAIndicator;
import org.ta4j.core.indicators.averages.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.VolumeIndicator;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IndicatorMapperTest {

    private BarSeries buildSeries(double... closes) {
        BarSeries series = new org.ta4j.core.BaseBarSeriesBuilder().build();
        Instant base = Instant.parse("2026-01-01T00:00:00Z");
        for (int i = 0; i < closes.length; i++) {
            Bar bar = new org.ta4j.core.BaseBar(
                    Duration.ofDays(1),
                    base.plus(java.time.Duration.ofDays(i)),
                    DecimalNum.valueOf(closes[i] * 0.98),
                    DecimalNum.valueOf(closes[i] * 1.02),
                    DecimalNum.valueOf(closes[i] * 0.98),
                    DecimalNum.valueOf(closes[i]),
                    DecimalNum.valueOf(1000.0 * (i + 1)),
                    DecimalNum.valueOf(0),
                    0
            );
            series.addBar(bar);
        }
        return series;
    }

    @Test
    void getDescriptions_returnsMap() {
        Map<String, String> descriptions = IndicatorMapper.getDescriptions();

        assertNotNull(descriptions);
        assertTrue(descriptions.size() > 0);
        assertTrue(descriptions.containsKey("close_50_sma"));
        assertTrue(descriptions.containsKey("rsi"));
        assertTrue(descriptions.containsKey("macd"));
    }

    @Test
    void getDescriptions_containsAllExpectedKeys() {
        Map<String, String> descriptions = IndicatorMapper.getDescriptions();

        assertTrue(descriptions.containsKey("close_50_sma"));
        assertTrue(descriptions.containsKey("close_200_sma"));
        assertTrue(descriptions.containsKey("macd"));
        assertTrue(descriptions.containsKey("rsi"));
        assertTrue(descriptions.containsKey("vwma"));
        assertTrue(descriptions.containsKey("atr"));
        assertTrue(descriptions.containsKey("boll"));
    }

    @Test
    void buildIndicator_close_50_sma() {
        BarSeries series = buildSeries(100.0, 101.0, 102.0);
        ClosePriceIndicator close = new ClosePriceIndicator(series);
        VolumeIndicator volume = new VolumeIndicator(series);

        Indicator<Num> indicator = IndicatorMapper.buildIndicator("close_50_sma", series, close, volume);

        assertNotNull(indicator);
        // SMA of 3 values with period 50 — should return NaN for index 0 (not enough data)
        assertDoesNotThrow(() -> indicator.getValue(0));
    }

    @Test
    void buildIndicator_close_200_sma() {
        BarSeries series = buildSeries(100.0);
        ClosePriceIndicator close = new ClosePriceIndicator(series);
        VolumeIndicator volume = new VolumeIndicator(series);

        Indicator<Num> indicator = IndicatorMapper.buildIndicator("close_200_sma", series, close, volume);

        assertNotNull(indicator);
    }

    @Test
    void buildIndicator_close_10_ema() {
        BarSeries series = buildSeries(100.0, 101.0, 102.0);
        ClosePriceIndicator close = new ClosePriceIndicator(series);
        VolumeIndicator volume = new VolumeIndicator(series);

        Indicator<Num> indicator = IndicatorMapper.buildIndicator("close_10_ema", series, close, volume);

        assertNotNull(indicator);
    }

    @Test
    void buildIndicator_macd() {
        BarSeries series = buildSeries(100.0, 101.0, 102.0, 103.0, 104.0);
        ClosePriceIndicator close = new ClosePriceIndicator(series);
        VolumeIndicator volume = new VolumeIndicator(series);

        Indicator<Num> indicator = IndicatorMapper.buildIndicator("macd", series, close, volume);

        assertNotNull(indicator);
    }

    @Test
    void buildIndicator_macds() {
        BarSeries series = buildSeries(100.0, 101.0, 102.0, 103.0, 104.0);
        ClosePriceIndicator close = new ClosePriceIndicator(series);
        VolumeIndicator volume = new VolumeIndicator(series);

        Indicator<Num> indicator = IndicatorMapper.buildIndicator("macds", series, close, volume);

        assertNotNull(indicator);
    }

    @Test
    void buildIndicator_macdh() {
        BarSeries series = buildSeries(100.0, 101.0, 102.0, 103.0, 104.0);
        ClosePriceIndicator close = new ClosePriceIndicator(series);
        VolumeIndicator volume = new VolumeIndicator(series);

        Indicator<Num> indicator = IndicatorMapper.buildIndicator("macdh", series, close, volume);

        assertNotNull(indicator);
    }

    @Test
    void buildIndicator_rsi() {
        BarSeries series = buildSeries(100.0, 101.0, 102.0, 103.0, 104.0);
        ClosePriceIndicator close = new ClosePriceIndicator(series);
        VolumeIndicator volume = new VolumeIndicator(series);

        Indicator<Num> indicator = IndicatorMapper.buildIndicator("rsi", series, close, volume);

        assertNotNull(indicator);
        assertTrue(indicator instanceof RSIIndicator);
    }

    @Test
    void buildIndicator_vwma() {
        BarSeries series = buildSeries(100.0, 101.0, 102.0);
        ClosePriceIndicator close = new ClosePriceIndicator(series);
        VolumeIndicator volume = new VolumeIndicator(series);

        Indicator<Num> indicator = IndicatorMapper.buildIndicator("vwma", series, close, volume);

        assertNotNull(indicator);
    }

    @Test
    void buildIndicator_atr() {
        BarSeries series = buildSeries(100.0, 101.0, 102.0);
        ClosePriceIndicator close = new ClosePriceIndicator(series);
        VolumeIndicator volume = new VolumeIndicator(series);

        Indicator<Num> indicator = IndicatorMapper.buildIndicator("atr", series, close, volume);

        assertNotNull(indicator);
        assertTrue(indicator instanceof ATRIndicator);
    }

    @Test
    void buildIndicator_boll() {
        BarSeries series = buildSeries(100.0, 101.0, 102.0);
        ClosePriceIndicator close = new ClosePriceIndicator(series);
        VolumeIndicator volume = new VolumeIndicator(series);

        Indicator<Num> indicator = IndicatorMapper.buildIndicator("boll", series, close, volume);

        assertNotNull(indicator);
        assertTrue(indicator instanceof SMAIndicator);
    }

    @Test
    void buildIndicator_unknownCodeThrows() {
        BarSeries series = buildSeries(100.0);
        ClosePriceIndicator close = new ClosePriceIndicator(series);
        VolumeIndicator volume = new VolumeIndicator(series);

        assertThrows(IllegalArgumentException.class, () ->
                IndicatorMapper.buildIndicator("unknown_code", series, close, volume));
    }

    @Test
    void buildIndicator_emptyCodeThrows() {
        BarSeries series = buildSeries(100.0);
        ClosePriceIndicator close = new ClosePriceIndicator(series);
        VolumeIndicator volume = new VolumeIndicator(series);

        assertThrows(IllegalArgumentException.class, () ->
                IndicatorMapper.buildIndicator("", series, close, volume));
    }
}
