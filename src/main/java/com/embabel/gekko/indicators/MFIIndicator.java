package com.embabel.gekko.indicators;

import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;

import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.AbstractIndicator;
import org.ta4j.core.num.Num;
import org.ta4j.core.num.NumFactory;

/**
 * Money Flow Index (MFI) over a rolling period
 */
@RegisterReflectionForBinding(MFIIndicator.class)
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
        NumFactory nf = series.numFactory();

        Num positiveFlow = nf.zero();
        Num negativeFlow = nf.zero();

        for (int i = start + 1; i <= index; i++) {
            Bar prev = series.getBar(i - 1);
            Bar curr = series.getBar(i);

            Num typicalPrev = prev.getHighPrice().plus(prev.getLowPrice()).plus(prev.getClosePrice())
                    .dividedBy(nf.three());
            Num typicalCurr = curr.getHighPrice().plus(curr.getLowPrice()).plus(curr.getClosePrice())
                    .dividedBy(nf.three());

            Num rawFlow = typicalCurr.multipliedBy(curr.getVolume());

            if (typicalCurr.isGreaterThan(typicalPrev)) {
                positiveFlow = positiveFlow.plus(rawFlow);
            } else if (typicalCurr.isLessThan(typicalPrev)) {
                negativeFlow = negativeFlow.plus(rawFlow);
            }
            // if equal, no flow counted
        }

        if (negativeFlow.isZero()) return nf.hundred(); // maxed out
        Num moneyRatio = positiveFlow.dividedBy(negativeFlow);
        return nf.hundred().minus(nf.hundred().dividedBy(moneyRatio.plus(nf.one())));
    }

    @Override
    public int getCountOfUnstableBars() {
        return period; // MFI needs 'period' bars to stabilize
    }
}
