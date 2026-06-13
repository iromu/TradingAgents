package com.embabel.gekko.indicators;

import org.junit.jupiter.api.Test;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class MFIIndicatorTest {

    private BarSeries buildSeries(double[] closes, double[] highs, double[] lows, double[] volumes) {
        BarSeries series = new org.ta4j.core.BaseBarSeriesBuilder().build();
        Instant base = Instant.parse("2026-01-01T00:00:00Z");
        for (int i = 0; i < closes.length; i++) {
            Bar bar = new org.ta4j.core.BaseBar(
                    Duration.ofDays(1),
                    base.plus(java.time.Duration.ofDays(i)),
                    DecimalNum.valueOf(highs[i]),
                    DecimalNum.valueOf(highs[i] * 1.01),
                    DecimalNum.valueOf(lows[i]),
                    DecimalNum.valueOf(closes[i]),
                    DecimalNum.valueOf(volumes[i]),
                    DecimalNum.valueOf(0),
                    0
            );
            series.addBar(bar);
        }
        return series;
    }

    @Test
    void getValue_upwardTrend_returnsHigh() {
        // Strictly increasing typical prices — all positive flow, MFI should be 100
        double[] closes = {10, 20, 30, 40, 50};
        double[] highs = {11, 21, 31, 41, 51};
        double[] lows = {9, 19, 29, 39, 49};
        double[] volumes = {100, 100, 100, 100, 100};

        BarSeries series = buildSeries(closes, highs, lows, volumes);
        MFIIndicator indicator = new MFIIndicator(series, 5);

        Num value = indicator.getValue(4);
        assertEquals(100.0, value.doubleValue(), 0.01);
    }

    @Test
    void getValue_downwardTrend_returnsLow() {
        // Strictly decreasing typical prices — all negative flow, MFI should be 0
        double[] closes = {50, 40, 30, 20, 10};
        double[] highs = {51, 41, 31, 21, 11};
        double[] lows = {49, 39, 29, 19, 9};
        double[] volumes = {100, 100, 100, 100, 100};

        BarSeries series = buildSeries(closes, highs, lows, volumes);
        MFIIndicator indicator = new MFIIndicator(series, 5);

        Num value = indicator.getValue(4);
        assertEquals(0.0, value.doubleValue(), 0.01);
    }

    @Test
    void getValue_mixedPrices_returnsBetween0And100() {
        // Alternating up/down — MFI should be between 0 and 100
        double[] closes = {10, 15, 12, 18, 14};
        double[] highs = {11, 16, 13, 19, 15};
        double[] lows = {9, 14, 11, 17, 13};
        double[] volumes = {100, 100, 100, 100, 100};

        BarSeries series = buildSeries(closes, highs, lows, volumes);
        MFIIndicator indicator = new MFIIndicator(series, 5);

        Num value = indicator.getValue(4);
        assertTrue(value.doubleValue() >= 0);
        assertTrue(value.doubleValue() <= 100);
    }

    @Test
    void getValue_neutralPrices_returns100() {
        // All prices the same — no positive or negative flow, code returns 100 (maxed)
        double[] closes = {10, 10, 10, 10, 10};
        double[] highs = {11, 11, 11, 11, 11};
        double[] lows = {9, 9, 9, 9, 9};
        double[] volumes = {100, 100, 100, 100, 100};

        BarSeries series = buildSeries(closes, highs, lows, volumes);
        MFIIndicator indicator = new MFIIndicator(series, 5);

        Num value = indicator.getValue(4);
        assertEquals(100.0, value.doubleValue(), 0.01);
    }

    @Test
    void getValue_firstValue_needsPreviousBar() {
        // Index 0: start=0, loop i from 1 to 0 (empty), all zero flow
        double[] closes = {10, 20, 30};
        double[] highs = {11, 21, 31};
        double[] lows = {9, 19, 29};
        double[] volumes = {100, 100, 100};

        BarSeries series = buildSeries(closes, highs, lows, volumes);
        MFIIndicator indicator = new MFIIndicator(series, 5);

        // With only one bar, there's no previous bar to compare — returns 100 (maxed)
        Num value = indicator.getValue(0);
        assertEquals(100.0, value.doubleValue(), 0.01);
    }

    @Test
    void getValue_periodRollsBack() {
        double[] closes = {10, 10, 50, 50};
        double[] highs = {11, 11, 51, 51};
        double[] lows = {9, 9, 49, 49};
        double[] volumes = {100, 100, 100, 100};

        BarSeries series = buildSeries(closes, highs, lows, volumes);
        MFIIndicator indicator = new MFIIndicator(series, 2);

        assertDoesNotThrow(() -> indicator.getValue(3));
    }

    @Test
    void getCountOfUnstableBars_returnsPeriod() {
        double[] closes = {10, 20, 30};
        double[] highs = {11, 21, 31};
        double[] lows = {9, 19, 29};
        double[] volumes = {100, 100, 100};

        BarSeries series = buildSeries(closes, highs, lows, volumes);
        MFIIndicator indicator = new MFIIndicator(series, 5);
        assertEquals(5, indicator.getCountOfUnstableBars());
    }
}
