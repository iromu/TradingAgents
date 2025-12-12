package com.embabel.gekko.indicators;

import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.AbstractIndicator;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;

/**
 * Money Flow Index (MFI) over a rolling period
 */
public class MFIIndicator extends AbstractIndicator<Num> {

    private final BarSeries series;
    private final int period;

    public MFIIndicator(BarSeries series, int period) {
        super(series);
        this.series = series;
        this.period = period;
    }

    @Override
    public Num getValue(int index) {
        int start = Math.max(series.getBeginIndex(), index - period + 1);

        Num positiveFlow = DecimalNum.valueOf(0);
        Num negativeFlow = DecimalNum.valueOf(0);

        for (int i = start + 1; i <= index; i++) {
            Bar prev = series.getBar(i - 1);
            Bar curr = series.getBar(i);

            Num typicalPrev = prev.getHighPrice().plus(prev.getLowPrice()).plus(prev.getClosePrice())
                    .dividedBy(DecimalNum.valueOf(3));
            Num typicalCurr = curr.getHighPrice().plus(curr.getLowPrice()).plus(curr.getClosePrice())
                    .dividedBy(DecimalNum.valueOf(3));

            Num rawFlow = typicalCurr.multipliedBy(curr.getVolume());

            if (typicalCurr.isGreaterThan(typicalPrev)) {
                positiveFlow = positiveFlow.plus(rawFlow);
            } else if (typicalCurr.isLessThan(typicalPrev)) {
                negativeFlow = negativeFlow.plus(rawFlow);
            }
            // if equal, no flow counted
        }

        if (negativeFlow.isZero()) return DecimalNum.valueOf(100); // maxed out
        Num moneyRatio = positiveFlow.dividedBy(negativeFlow);
        return DecimalNum.valueOf(100).minus(DecimalNum.valueOf(100).dividedBy(moneyRatio.plus(DecimalNum.valueOf(1))));
    }

    @Override
    public int getCountOfUnstableBars() {
        return period; // MFI needs 'period' bars to stabilize
    }
}
