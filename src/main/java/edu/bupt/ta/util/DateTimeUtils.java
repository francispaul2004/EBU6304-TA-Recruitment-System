package edu.bupt.ta.util;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class DateTimeUtils {

    private DateTimeUtils() {
    }

    public static LocalDateTime now() {
        return LocalDateTime.now();
    }

    public static LocalDate today() {
        return LocalDate.now();
    }
}
