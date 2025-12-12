package com.embabel.gekko.indicators;


import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.AbstractIndicator;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;

/**
 * Rolling-window VWAP (typical price * volume / sum(volume))
 */
public class VWAPIndicator extends AbstractIndicator<Num> {

    private final BarSeries series;
    private final int period;

    public VWAPIndicator(BarSeries series, int period) {
        super(series);
        this.series = series;
        this.period = period;
    }

    @Override
    public Num getValue(int index) {
        int start = Math.max(series.getBeginIndex(), index - period + 1);
        Num pv = DecimalNum.valueOf(0);   // zero
        Num volSum = DecimalNum.valueOf(0);

        for (int i = start; i <= index; i++) {
            Bar b = series.getBar(i);
            Num typical = b.getHighPrice().plus(b.getLowPrice()).plus(b.getClosePrice())
                    .dividedBy(DecimalNum.valueOf(3));
            Num vol = b.getVolume();
            pv = pv.plus(typical.multipliedBy(vol));
            volSum = volSum.plus(vol);
        }
        if (volSum.isZero()) return DecimalNum.valueOf(Double.NaN);
        return pv.dividedBy(volSum);
    }

    @Override
    public int getCountOfUnstableBars() {
        // VWAP has no unstable bars
        return 0;
    }
}
