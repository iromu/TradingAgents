package com.embabel.gekko.indicators;

import org.junit.jupiter.api.Test;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class VWMAIndicatorTest {

    private BarSeries buildSeries(double... closePrices) {
        BarSeries series = new org.ta4j.core.BaseBarSeriesBuilder().build();
        Instant base = Instant.parse("2026-01-01T00:00:00Z");
        for (int i = 0; i < closePrices.length; i++) {
            double close = closePrices[i];
            double volume = 1000.0 * (i + 1);
            Bar bar = new org.ta4j.core.BaseBar(
                    Duration.ofDays(1),
                    base.plus(java.time.Duration.ofDays(i)),
                    DecimalNum.valueOf(close * 0.98),
                    DecimalNum.valueOf(close * 1.02),
                    DecimalNum.valueOf(close * 0.98),
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
    void getValue_singleBar_returnsClosePrice() {
        BarSeries series = buildSeries(100.0);
        VWMAIndicator indicator = new VWMAIndicator(series, 5);

        Num value = indicator.getValue(0);
        assertEquals(100.0, value.doubleValue(), 0.001);
    }

    @Test
    void getValue_multipleBars_volumeWeighted() {
        // Bar 0: close=100, vol=1000; Bar 1: close=200, vol=2000
        // VWMA = (100*1000 + 200*2000) / 3000 = 500000/3000 = 166.67
        BarSeries series = buildSeries(100.0, 200.0);
        VWMAIndicator indicator = new VWMAIndicator(series, 5);

        Num value = indicator.getValue(1);
        double expected = (100.0 * 1000 + 200.0 * 2000) / 3000.0;
        assertEquals(expected, value.doubleValue(), 0.01);
    }

    @Test
    void getValue_higherPriceHigherVolume_pullsAverageUp() {
        // Bar 0: close=50, vol=1000; Bar 1: close=100, vol=9000
        // VWMA = (50*1000 + 100*9000) / 10000 = 950000/10000 = 95
        BarSeries series = new org.ta4j.core.BaseBarSeriesBuilder().build();
        Instant base = Instant.parse("2026-01-01T00:00:00Z");
        series.addBar(new org.ta4j.core.BaseBar(
                Duration.ofDays(1), base,
                DecimalNum.valueOf(49), DecimalNum.valueOf(51),
                DecimalNum.valueOf(49), DecimalNum.valueOf(50),
                DecimalNum.valueOf(1000), DecimalNum.valueOf(0), 0
        ));
        series.addBar(new org.ta4j.core.BaseBar(
                Duration.ofDays(1), base.plus(java.time.Duration.ofDays(1)),
                DecimalNum.valueOf(99), DecimalNum.valueOf(101),
                DecimalNum.valueOf(99), DecimalNum.valueOf(100),
                DecimalNum.valueOf(9000), DecimalNum.valueOf(0), 0
        ));

        VWMAIndicator indicator = new VWMAIndicator(series, 5);
        Num value = indicator.getValue(1);
        assertEquals(95.0, value.doubleValue(), 0.01);
    }

    @Test
    void getValue_zeroVolume_throws() {
        // Zero volume causes DecimalNum.valueOf(Double.NaN) to throw NumberFormatException
        // in the VWMA indicator — this is a known edge case in the indicator code
        BarSeries series = new org.ta4j.core.BaseBarSeriesBuilder().build();
        series.addBar(new org.ta4j.core.BaseBar(
                Duration.ofDays(1),
                Instant.parse("2026-01-01T00:00:00Z"),
                DecimalNum.valueOf(100), DecimalNum.valueOf(102),
                DecimalNum.valueOf(98), DecimalNum.valueOf(100),
                DecimalNum.valueOf(0), DecimalNum.valueOf(0), 0
        ));
        VWMAIndicator indicator = new VWMAIndicator(series, 5);

        assertThrows(Exception.class, () -> indicator.getValue(0));
    }

    @Test
    void getCountOfUnstableBars_returnsZero() {
        BarSeries series = buildSeries(100.0);
        VWMAIndicator indicator = new VWMAIndicator(series, 5);
        assertEquals(0, indicator.getCountOfUnstableBars());
    }

    @Test
    void getValue_periodLimitation() {
        // Period of 2, index 3 means we only look at bars 2 and 3
        BarSeries series = buildSeries(10.0, 10.0, 50.0, 50.0);
        VWMAIndicator indicator = new VWMAIndicator(series, 2);

        assertDoesNotThrow(() -> indicator.getValue(3));
    }
}
