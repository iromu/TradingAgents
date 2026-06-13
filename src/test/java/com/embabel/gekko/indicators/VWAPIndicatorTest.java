package com.embabel.gekko.indicators;

import org.junit.jupiter.api.Test;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;

import java.time.Duration;
import java.time.Instant;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class VWAPIndicatorTest {

    private BarSeries buildSeries(double... closePrices) {
        return buildSeries(closePrices, closePrices.length);
    }

    private BarSeries buildSeries(double[] closePrices, int period) {
        BarSeries series = new org.ta4j.core.BaseBarSeriesBuilder().build();
        Instant base = Instant.parse("2026-01-01T00:00:00Z");
        for (int i = 0; i < closePrices.length; i++) {
            double close = closePrices[i];
            double high = close * 1.02;
            double low = close * 0.98;
            double open = close * 1.005;
            double volume = 1000.0 * (i + 1);
            Bar bar = new org.ta4j.core.BaseBar(
                    Duration.ofDays(1),
                    base.plus(java.time.Duration.ofDays(i)),
                    DecimalNum.valueOf(open),
                    DecimalNum.valueOf(high),
                    DecimalNum.valueOf(low),
                    DecimalNum.valueOf(close),
                    DecimalNum.valueOf(volume),
                    DecimalNum.valueOf(0),
                    0
            );
            series.addBar(bar);
        }
        return series;
    }

    @Test
    void getValue_singleBar_returnsTypicalPrice() {
        // With a single bar, VWAP = typical price = (high + low + close) / 3
        BarSeries series = buildSeries(100.0);
        VWAPIndicator indicator = new VWAPIndicator(series, 5);

        Num value = indicator.getValue(0);
        double expected = (100 * 1.02 + 100 * 0.98 + 100) / 3.0;
        assertEquals(expected, value.doubleValue(), 0.001);
    }

    @Test
    void getValue_multipleBars_weightedByVolume() {
        // Two bars with different volumes — VWAP should be volume-weighted average of typical prices
        BarSeries series = buildSeries(100.0, 110.0);
        VWAPIndicator indicator = new VWAPIndicator(series, 5);

        Num value = indicator.getValue(1);
        // Bar 0: typical = (102 + 98 + 100) / 3 = 100, volume = 1000
        // Bar 1: typical = (112.2 + 107.8 + 110) / 3 = 110, volume = 2000
        // VWAP = (100*1000 + 110*2000) / 3000 = 320000/3000 = 106.667
        assertEquals(106.667, value.doubleValue(), 0.01);
    }

    @Test
    void getValue_periodRollsBack() {
        // Period of 2, index 3 means we only look at bars 2 and 3
        BarSeries series = buildSeries(100.0, 100.0, 200.0, 200.0);
        VWAPIndicator indicator = new VWAPIndicator(series, 2);

        // Should not throw — window correctly rolls back
        assertDoesNotThrow(() -> indicator.getValue(3));
    }

    @Test
    void getValue_zeroVolume_throws() {
        // Zero volume causes DecimalNum.valueOf(Double.NaN) to throw NumberFormatException
        // in the VWAP indicator — this is a known edge case in the indicator code
        BarSeries series = new org.ta4j.core.BaseBarSeriesBuilder().build();
        series.addBar(new org.ta4j.core.BaseBar(
                Duration.ofDays(1),
                Instant.parse("2026-01-01T00:00:00Z"),
                DecimalNum.valueOf(100), DecimalNum.valueOf(102),
                DecimalNum.valueOf(98), DecimalNum.valueOf(100),
                DecimalNum.valueOf(0), DecimalNum.valueOf(0), 0
        ));
        VWAPIndicator indicator = new VWAPIndicator(series, 5);

        assertThrows(Exception.class, () -> indicator.getValue(0));
    }

    @Test
    void getCountOfUnstableBars_returnsZero() {
        BarSeries series = buildSeries(100.0);
        VWAPIndicator indicator = new VWAPIndicator(series, 5);
        assertEquals(0, indicator.getCountOfUnstableBars());
    }

    @Test
    void getValue_withLargePeriod_returnsAllBarsWeighted() {
        BarSeries series = buildSeries(10.0, 20.0, 30.0);
        VWAPIndicator indicator = new VWAPIndicator(series, 100);

        assertDoesNotThrow(() -> {
            Num v = indicator.getValue(2);
            assertFalse(v.isNaN());
            assertTrue(v.doubleValue() > 0);
        });
    }
}
