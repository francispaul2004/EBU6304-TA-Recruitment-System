package edu.bupt.ta.util;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class DateTimeUtils {

    private static final String FIXED_NOW_PROPERTY = "ta.fixed.now";
    private static final String FIXED_TODAY_PROPERTY = "ta.fixed.today";

    private DateTimeUtils() {
    }

    public static LocalDateTime now() {
        String fixedNow = System.getProperty(FIXED_NOW_PROPERTY);
        if (fixedNow != null && !fixedNow.isBlank()) {
            return LocalDateTime.parse(fixedNow);
        }

        String fixedToday = System.getProperty(FIXED_TODAY_PROPERTY);
        if (fixedToday != null && !fixedToday.isBlank()) {
            return LocalDate.parse(fixedToday).atStartOfDay();
        }
        return LocalDateTime.now();
    }

    public static LocalDate today() {
        String fixedToday = System.getProperty(FIXED_TODAY_PROPERTY);
        if (fixedToday != null && !fixedToday.isBlank()) {
            return LocalDate.parse(fixedToday);
        }

        String fixedNow = System.getProperty(FIXED_NOW_PROPERTY);
        if (fixedNow != null && !fixedNow.isBlank()) {
            return LocalDateTime.parse(fixedNow).toLocalDate();
        }
        return LocalDate.now();
    }
}
