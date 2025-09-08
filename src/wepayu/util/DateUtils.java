package wepayu.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DateUtils {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static LocalDate parse(String date) {
        return LocalDate.parse(date, FORMATTER);
    }

    public static boolean isFriday(String date) {
        LocalDate d = parse(date);
        return d.getDayOfWeek() == DayOfWeek.FRIDAY;
    }

    public static boolean isLastWorkDayOfMonth(String date) {
        LocalDate d = parse(date);
        LocalDate lastDay = d.withDayOfMonth(d.lengthOfMonth());
        while (lastDay.getDayOfWeek() == DayOfWeek.SATURDAY || lastDay.getDayOfWeek() == DayOfWeek.SUNDAY) {
            lastDay = lastDay.minusDays(1);
        }
        return d.equals(lastDay);
    }

    public static boolean isBiweeklyFriday(String date) {
        // Supondo que a 1ª sexta-feira do ano seja referência
        LocalDate d = parse(date);
        if (d.getDayOfWeek() != DayOfWeek.FRIDAY) return false;

        LocalDate firstFriday = LocalDate.of(d.getYear(), 1, 1);
        while (firstFriday.getDayOfWeek() != DayOfWeek.FRIDAY) {
            firstFriday = firstFriday.plusDays(1);
        }

        long weeks = java.time.temporal.ChronoUnit.WEEKS.between(firstFriday, d);
        return weeks % 2 == 0;
    }
}
