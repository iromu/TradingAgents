package com.embabel.gekko.indicators;

import org.junit.jupiter.api.Test;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Indicator;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class SubtractIndicatorTest {

    private BarSeries buildSeries(double... values) {
        BarSeries series = new org.ta4j.core.BaseBarSeriesBuilder().build();
        Instant base = Instant.parse("2026-01-01T00:00:00Z");
        for (int i = 0; i < values.length; i++) {
            Bar bar = new org.ta4j.core.BaseBar(
                    Duration.ofDays(1),
                    base.plus(java.time.Duration.ofDays(i)),
                    DecimalNum.valueOf(values[i] * 0.98),
                    DecimalNum.valueOf(values[i] * 1.02),
                    DecimalNum.valueOf(values[i] * 0.98),
                    DecimalNum.valueOf(values[i]),
                    DecimalNum.valueOf(1000),
                    DecimalNum.valueOf(0),
                    0
            );
            series.addBar(bar);
        }
        return series;
    }

    @Test
    void getValue_simpleSubtraction() {
        BarSeries series = buildSeries(100.0, 200.0, 300.0);

        Indicator<Num> left = new org.ta4j.core.indicators.helpers.ClosePriceIndicator(series);
        Indicator<Num> right = new org.ta4j.core.indicators.helpers.ClosePriceIndicator(series);

        // Right is shifted: use a constant indicator at 10
        SubtractIndicator indicator = new SubtractIndicator(left, new ConstantIndicator(10));

        assertEquals(90.0, indicator.getValue(0).doubleValue(), 0.001);
        assertEquals(190.0, indicator.getValue(1).doubleValue(), 0.001);
        assertEquals(290.0, indicator.getValue(2).doubleValue(), 0.001);
    }

    @Test
    void getValue_twoClosePriceIndicators_returnsZero() {
        BarSeries series = buildSeries(100.0, 200.0, 300.0);

        Indicator<Num> close = new org.ta4j.core.indicators.helpers.ClosePriceIndicator(series);
        SubtractIndicator indicator = new SubtractIndicator(close, close);

        // Same indicator subtracted from itself = 0
        assertEquals(0.0, indicator.getValue(0).doubleValue(), 0.001);
        assertEquals(0.0, indicator.getValue(1).doubleValue(), 0.001);
        assertEquals(0.0, indicator.getValue(2).doubleValue(), 0.001);
    }

    @Test
    void getValue_negativeResult() {
        BarSeries series = buildSeries(10.0, 20.0, 30.0);

        Indicator<Num> left = new ConstantIndicator(5);
        Indicator<Num> right = new org.ta4j.core.indicators.helpers.ClosePriceIndicator(series);
        SubtractIndicator indicator = new SubtractIndicator(left, right);

        assertEquals((-5.0), indicator.getValue(0).doubleValue(), 0.001);
        assertEquals((-15.0), indicator.getValue(1).doubleValue(), 0.001);
        assertEquals((-25.0), indicator.getValue(2).doubleValue(), 0.001);
    }

    @Test
    void getCountOfUnstableBars_returnsMaxOfBoth() {
        BarSeries series = buildSeries(100.0);

        Indicator<Num> left = new ConstantIndicator(0);
        Indicator<Num> right = new ConstantIndicator(0);
        SubtractIndicator indicator = new SubtractIndicator(left, right);

        assertEquals(0, indicator.getCountOfUnstableBars());
    }

    @Test
    void getValue_twoConstants() {
        Indicator<Num> left = new ConstantIndicator(100);
        Indicator<Num> right = new ConstantIndicator(30);
        SubtractIndicator indicator = new SubtractIndicator(left, right);

        assertEquals(70.0, indicator.getValue(0).doubleValue(), 0.001);
    }

    /**
     * Simple constant-value indicator for testing.
     */
    private static class ConstantIndicator implements Indicator<Num> {
        private final Num value;

        ConstantIndicator(double v) {
            this.value = DecimalNum.valueOf(v);
        }

        @Override
        public Num getValue(int index) {
            return value;
        }

        @Override
        public int getCountOfUnstableBars() {
            return 0;
        }

        @Override
        public BarSeries getBarSeries() {
            return new org.ta4j.core.BaseBarSeriesBuilder().build();
        }
    }
}
