package com.embabel.template.indicators;

import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.AbstractIndicator;
import org.ta4j.core.num.Num;

/**
 * Subtracts two indicators: left - right
 */
public class SubtractIndicator extends AbstractIndicator<Num> {

    private final Indicator<Num> left;
    private final Indicator<Num> right;

    public SubtractIndicator(Indicator<Num> left, Indicator<Num> right) {
        super(left.getBarSeries());
        this.left = left;
        this.right = right;
    }

    @Override
    public Num getValue(int index) {
        return left.getValue(index).minus(right.getValue(index));
    }

    @Override
    public int getCountOfUnstableBars() {
        return Math.max(left.getCountOfUnstableBars(), right.getCountOfUnstableBars());
    }
}
