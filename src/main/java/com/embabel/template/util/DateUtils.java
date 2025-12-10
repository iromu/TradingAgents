package com.embabel.template.util;


import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.Date;

public class DateUtils {
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

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
        ZonedDateTime zdt = date.atStartOfDay(ZoneId.systemDefault());
        Calendar cal = Calendar.getInstance();
        cal.setTime(Date.from(zdt.toInstant()));
        return cal;
    }

}
