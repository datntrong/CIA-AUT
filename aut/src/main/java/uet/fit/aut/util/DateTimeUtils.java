package uet.fit.aut.util;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateTimeUtils {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM uuuu");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm:ss a");

    public static String getDate(@NotNull LocalDateTime dateTime) {
        return DATE_FORMATTER.format(dateTime).toUpperCase();
    }

    public static String getTime(@NotNull LocalDateTime dateTime) {
        return TIME_FORMATTER.format(dateTime).toUpperCase();
    }

    public static LocalDateTime parse(@NotNull String dateTime) {
        return LocalDateTime.parse(dateTime, DATE_TIME_FORMATTER);
    }

    public static String toString(@NotNull LocalDateTime dateTime) {
        return dateTime.format(DATE_TIME_FORMATTER);
    }
}
