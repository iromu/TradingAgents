package com.embabel.gekko.dataflows;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for YFinService data-quality null handling.
 *
 * <p>The spec requires that when historical data contains a null getOpen() (or
 * high/low/close/volume) value, the system uses null instead of Double.NaN so
 * that no DecimalNum value contains NaN. That logic lives in the private
 * loadBarSeries(), which depends on the YahooFinance external API and cannot be
 * unit tested without mocking the client. The toCsvNumber() helper is the
 * shared null-handling primitive used by the CSV output path, so it is tested
 * directly here via reflection.
 */
class YFinServiceNullHandlingTest {

    private final YFinService service = new YFinService();

    private String invokeToCsvNumber(BigDecimal v) throws Exception {
        Method m = YFinService.class.getDeclaredMethod("toCsvNumber", BigDecimal.class);
        m.setAccessible(true);
        return (String) m.invoke(service, v);
    }

    @Test
    void toCsvNumber_nullReturnsEmpty() throws Exception {
        assertEquals("", invokeToCsvNumber(null));
    }

    @Test
    void toCsvNumber_validValueFormatsCorrectly() throws Exception {
        assertEquals("150.50", invokeToCsvNumber(new BigDecimal("150.5")));
    }

    @Test
    void toCsvNumber_zeroFormatsWithTwoDecimals() throws Exception {
        assertEquals("0.00", invokeToCsvNumber(BigDecimal.ZERO));
    }

    @Test
    void toCsvNumber_roundsHalfUpToTwoDecimals() throws Exception {
        // 123.456 -> 123.46 with HALF_UP
        assertEquals("123.46", invokeToCsvNumber(new BigDecimal("123.456")));
        // 123.454 -> 123.45 with HALF_UP
        assertEquals("123.45", invokeToCsvNumber(new BigDecimal("123.454")));
    }

    @Test
    void toCsvNumber_negativeValueFormatsCorrectly() throws Exception {
        assertEquals("-99.99", invokeToCsvNumber(new BigDecimal("-99.99")));
    }

    @Test
    void toCsvNumber_usesPlainStringNoScientificNotation() throws Exception {
        // Large value would use scientific notation with toString(); toPlainString avoids that
        assertEquals("1000000.00", invokeToCsvNumber(new BigDecimal("1000000")));
    }
}
