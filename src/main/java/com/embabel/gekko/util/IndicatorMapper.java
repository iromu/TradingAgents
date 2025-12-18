package com.embabel.gekko.util;

import com.embabel.gekko.indicators.SubtractIndicator;
import com.embabel.gekko.indicators.VWAPIndicator;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.ATRIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.averages.EMAIndicator;
import org.ta4j.core.indicators.averages.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.VolumeIndicator;
import org.ta4j.core.num.Num;

import java.util.HashMap;
import java.util.Map;

public class IndicatorMapper {

    /**
     * Returns a map of indicator codes to their descriptions.
     */
    public static Map<String, String> getDescriptions() {
        Map<String, String> desc = new HashMap<>();
        desc.put("close_50_sma", "50 SMA: A medium-term trend indicator...");
        desc.put("close_200_sma", "200 SMA: A long-term trend benchmark...");
        desc.put("macd", "MACD: Computes momentum via differences of EMAs...");
        desc.put("rsi", "RSI: Measures momentum to flag overbought/oversold conditions...");
        desc.put("vwma", "VWMA: Volume Weighted Moving Average...");
        desc.put("atr", "ATR: Average True Range...");
        desc.put("boll", "Bollinger Bands middle line (20 SMA)...");
        // add other descriptions as needed
        return desc;
    }

    /**
     * Builds a TA4J indicator from a code string.
     */
    public static Indicator<Num> buildIndicator(String code, BarSeries series,
                                                ClosePriceIndicator close,
                                                VolumeIndicator volume) {

        switch (code) {

            case "close_50_sma":
                return new SMAIndicator(close, 50);

            case "close_200_sma":
                return new SMAIndicator(close, 200);

            case "close_10_ema":
                return new EMAIndicator(close, 10);

            case "macd": {
                EMAIndicator ema12 = new EMAIndicator(close, 12);
                EMAIndicator ema26 = new EMAIndicator(close, 26);
                return new SubtractIndicator(ema12, ema26); // replaces DifferenceIndicator
            }

            case "macds": {
                EMAIndicator ema12 = new EMAIndicator(close, 12);
                EMAIndicator ema26 = new EMAIndicator(close, 26);
                SubtractIndicator macdLine = new SubtractIndicator(ema12, ema26);
                return new EMAIndicator(macdLine, 9); // MACD signal line
            }

            case "macdh": {
                EMAIndicator ema12 = new EMAIndicator(close, 12);
                EMAIndicator ema26 = new EMAIndicator(close, 26);
                SubtractIndicator macdLine = new SubtractIndicator(ema12, ema26);
                EMAIndicator signalLine = new EMAIndicator(macdLine, 9);
                return new SubtractIndicator(macdLine, signalLine); // MACD histogram
            }

            case "rsi":
                return new RSIIndicator(close, 14);

            case "vwma":
                return new VWAPIndicator(series, 20); // your custom VWAP indicator

            case "atr":
                return new ATRIndicator(series, 14);

            case "boll":
                return new SMAIndicator(close, 20); // middle line; upper/lower bands can be built elsewhere

            // Add other indicators here...

            default:
                throw new IllegalArgumentException("Unsupported indicator code: " + code);
        }
    }
}
