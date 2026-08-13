package com.embabel.gekko.util;


import lombok.experimental.UtilityClass;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

@UtilityClass
public class DateUtils {
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Parse a date string in {@code yyyy-MM-dd} format (e.g., "2025-01-15").
     *
     * @param s the date string to parse
     * @return the parsed {@link LocalDate}
     * @throws IllegalArgumentException if {@code s} is null or not in yyyy-MM-dd format
     */
    public static LocalDate parseDate(String s) {
        if (s == null) throw new IllegalArgumentException("date string is null");
        try {
            return LocalDate.parse(s, DF);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Date must be in yyyy-MM-dd format: " + s, ex);
        }
    }

    public static String formatDate(LocalDate d) {
        return d.format(DF);
    }

    public static Calendar toCalendar(LocalDate date) {
        ZonedDateTime zdt = date.atStartOfDay(ZoneOffset.UTC);
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTime(Date.from(zdt.toInstant()));
        return cal;
    }

}
