package wepayu.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;

public class DateUtils {
    // ---------------------------------------------------------------
    // Compara duas datas: retorna <0 se d1<d2, 0 se igual, >0 se d1>d2
    // ---------------------------------------------------------------
    public static int compareDates(String d1, String d2) {
        LocalDate date1 = LocalDate.parse(d1.trim(), FLEXIBLE_FORMATTER);
        LocalDate date2 = LocalDate.parse(d2.trim(), FLEXIBLE_FORMATTER);
        return date1.compareTo(date2);
    }

    private static final DateTimeFormatter FLEXIBLE_FORMATTER =
            new DateTimeFormatterBuilder()
                    .appendValue(ChronoField.DAY_OF_MONTH, 1, 2, java.time.format.SignStyle.NOT_NEGATIVE)
                    .appendLiteral('/')
                    .appendValue(ChronoField.MONTH_OF_YEAR, 1, 2, java.time.format.SignStyle.NOT_NEGATIVE)
                    .appendLiteral('/')
                    .appendValue(ChronoField.YEAR, 4)
                    .toFormatter()
                    .withResolverStyle(ResolverStyle.STRICT);

            // Expose a parser for other utilities
            public static java.time.LocalDate parseLocalDate(String dateStr) {
                return LocalDate.parse(dateStr.trim(), FLEXIBLE_FORMATTER);
            }

    // ---------------------------------------------------------------
    // Verifica se uma data é válida (aceita 1/1/2005 e 01/01/2005)
    // ---------------------------------------------------------------
    public static boolean isValidDate(String dateStr) {
        try {
            LocalDate.parse(dateStr.trim(), FLEXIBLE_FORMATTER);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    // ---------------------------------------------------------------
    // Retorna true se d1 > d2
    // ---------------------------------------------------------------
    public static boolean isAfter(String d1, String d2) {
        LocalDate date1 = LocalDate.parse(d1.trim(), FLEXIBLE_FORMATTER);
        LocalDate date2 = LocalDate.parse(d2.trim(), FLEXIBLE_FORMATTER);
        return date1.isAfter(date2);
    }

    // ---------------------------------------------------------------
    // Entre duas datas (inclusivo)
    // ---------------------------------------------------------------
    public static boolean isBetweenInclusive(String date, String start, String end) {
        LocalDate d = LocalDate.parse(date.trim(), FLEXIBLE_FORMATTER);
        LocalDate s = LocalDate.parse(start.trim(), FLEXIBLE_FORMATTER);
        LocalDate e = LocalDate.parse(end.trim(), FLEXIBLE_FORMATTER);
        return (!d.isBefore(s)) && (!d.isAfter(e));
    }

    // ---------------------------------------------------------------
    // Entre duas datas (exclusivo no final)
    // ---------------------------------------------------------------
    public static boolean isBetweenExclusiveEnd(String date, String start, String end) {
        LocalDate d = LocalDate.parse(date.trim(), FLEXIBLE_FORMATTER);
        LocalDate s = LocalDate.parse(start.trim(), FLEXIBLE_FORMATTER);
        LocalDate e = LocalDate.parse(end.trim(), FLEXIBLE_FORMATTER);
        return (!d.isBefore(s)) && d.isBefore(e);
    }

    // ---------------------------------------------------------------
    // Sexta-feira simples
    // ---------------------------------------------------------------
    public static boolean isFriday(String dateStr) {
        LocalDate d = LocalDate.parse(dateStr.trim(), FLEXIBLE_FORMATTER);
        return d.getDayOfWeek().getValue() == 5;
    }

    // ---------------------------------------------------------------
    // Último dia útil do mês (sexta ou antes)
    // ---------------------------------------------------------------
    public static boolean isLastWorkDayOfMonth(String dateStr) {
        LocalDate d = LocalDate.parse(dateStr.trim(), FLEXIBLE_FORMATTER);
        LocalDate lastDay = d.withDayOfMonth(d.lengthOfMonth());
        while (lastDay.getDayOfWeek().getValue() > 5) {
            lastDay = lastDay.minusDays(1);
        }
        return d.equals(lastDay);
    }

    // ---------------------------------------------------------------
    // Sexta-feira a cada duas semanas (bi-semanal)
    // ---------------------------------------------------------------
    public static boolean isBiweeklyFriday(String dateStr) {
        LocalDate d = LocalDate.parse(dateStr.trim(), FLEXIBLE_FORMATTER);
        LocalDate base = LocalDate.of(2005, 1, 7); // sexta base
        long weeksBetween = java.time.temporal.ChronoUnit.WEEKS.between(base, d);
        return d.getDayOfWeek().getValue() == 5 && (weeksBetween % 2 == 0);
    }
}
