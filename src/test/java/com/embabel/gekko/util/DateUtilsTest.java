package com.embabel.gekko.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Calendar;

import static org.junit.jupiter.api.Assertions.*;

class DateUtilsTest {

    @Test
    void parseDate_validFormat() {
        LocalDate result = DateUtils.parseDate("2026-06-13");
        assertEquals(2026, result.getYear());
        assertEquals(6, result.getMonthValue());
        assertEquals(13, result.getDayOfMonth());
    }

    @Test
    void parseDate_leapYear() {
        LocalDate result = DateUtils.parseDate("2024-02-29");
        assertEquals(2024, result.getYear());
        assertEquals(2, result.getMonthValue());
        assertEquals(29, result.getDayOfMonth());
    }

    @Test
    void parseDate_nullThrows() {
        assertThrows(IllegalArgumentException.class, () -> DateUtils.parseDate(null));
    }

    @Test
    void parseDate_invalidFormatThrows() {
        assertThrows(IllegalArgumentException.class, () -> DateUtils.parseDate("13-06-2026"));
    }

    @Test
    void parseDate_emptyStringThrows() {
        assertThrows(IllegalArgumentException.class, () -> DateUtils.parseDate(""));
    }

    @Test
    void parseDate_invalidMonthThrows() {
        assertThrows(IllegalArgumentException.class, () -> DateUtils.parseDate("2026-13-01"));
    }

    @Test
    void formatDate_validDate() {
        String result = DateUtils.formatDate(LocalDate.of(2026, 6, 13));
        assertEquals("2026-06-13", result);
    }

    @Test
    void formatDate_singleDigitMonth() {
        String result = DateUtils.formatDate(LocalDate.of(2026, 1, 5));
        assertEquals("2026-01-05", result);
    }

    @Test
    void toCalendar_convertsCorrectly() {
        LocalDate date = LocalDate.of(2026, 6, 13);
        Calendar cal = DateUtils.toCalendar(date);

        assertEquals(2026, cal.get(Calendar.YEAR));
        // Calendar months are 0-indexed
        assertEquals(5, cal.get(Calendar.MONTH));
        assertEquals(13, cal.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    void toCalendar_midnightTime() {
        LocalDate date = LocalDate.of(2026, 6, 13);
        Calendar cal = DateUtils.toCalendar(date);

        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, cal.get(Calendar.MINUTE));
        assertEquals(0, cal.get(Calendar.SECOND));
    }
}
