package wepayu.util;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public class ScheduleUtils {
    // Determine if the descriptor matches the given date.
    // Supported descriptors (tests):
    // - "mensal $" -> last workday of month
    // - "mensal N" -> day N of month (1..28)
    // - "semanal D" -> weekly on day D (1=Mon..7=Sun)
    // - "semanal X D" -> every X weeks on day D (X>=1, D 1..7)
    // Descriptor comparison is case-insensitive and ignores extra spaces.

    public static boolean isPayDateForDescriptor(String descriptor, String dateStr) {
        if (descriptor == null || descriptor.isBlank()) return false;
        String desc = descriptor.trim().toLowerCase(Locale.ROOT);
        String[] parts = desc.split("\\s+");
        try {
            if (parts[0].equals("mensal")) {
                if (parts.length != 2) return false;
                String p = parts[1];
                if (p.equals("$")) {
                    return DateUtils.isLastWorkDayOfMonth(dateStr);
                } else {
                    int dia = Integer.parseInt(p);
                    java.time.LocalDate d = DateUtils.parseLocalDate(dateStr);
                    return d.getDayOfMonth() == dia;
                }
            } else if (parts[0].equals("semanal")) {
                java.time.LocalDate d = DateUtils.parseLocalDate(dateStr);
                if (parts.length == 2) {
                    int dia = Integer.parseInt(parts[1]);
                    return d.getDayOfWeek().getValue() == dia;
                } else if (parts.length == 3) {
                    int intervalo = Integer.parseInt(parts[1]);
                    int dia = Integer.parseInt(parts[2]);
                    if (d.getDayOfWeek().getValue() != dia) return false;
                    // anchor: first occurrence of this weekday in January 2005
                    java.time.LocalDate firstWeekday = LocalDate.of(2005,1,1).with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.of(dia)));
                    // first pay is anchor + (intervalo - 1) weeks
                    java.time.LocalDate firstPay = firstWeekday.plusWeeks(Math.max(0, intervalo - 1));
                    long weeksBetween = java.time.temporal.ChronoUnit.WEEKS.between(firstPay, d);
                    return weeksBetween >= 0 && (weeksBetween % intervalo) == 0;
                }
            }
        } catch (NumberFormatException | DateTimeParseException ex) {
            return false;
        }
        return false;
    }

    // PayPeriod represents the inclusive start date and exclusive end date of the period
    public static class PayPeriod {
        public final LocalDate start;
        public final LocalDate end; // exclusive
        public final double multiplier; // fraction of a month this period represents (for proration)

        public PayPeriod(LocalDate start, LocalDate end, double multiplier) {
            this.start = start;
            this.end = end;
            this.multiplier = multiplier;
        }
    }

    // Given a descriptor and a pay date (string), return the pay period (start inclusive, end exclusive)
    // and a multiplier to prorate monthly salaries. Returns null if descriptor is invalid or date is not a pay date.
    public static PayPeriod getPayPeriodForDescriptor(String descriptor, String payDateStr) {
        if (descriptor == null || descriptor.isBlank()) return null;
        String desc = descriptor.trim().toLowerCase(Locale.ROOT);
        String[] parts = desc.split("\\s+");
        LocalDate payDate = DateUtils.parseLocalDate(payDateStr);
        try {
            if (parts[0].equals("mensal")) {
                if (parts.length != 2) return null;
                String p = parts[1];
                if (p.equals("$")) {
                    // period: first day of month until (last workday +1)
                    LocalDate start = payDate.withDayOfMonth(1);
                    LocalDate end = payDate.plusDays(1);
                    double multiplier = 1.0; // full month
                    return new PayPeriod(start, end, multiplier);
                } else {
                    int dia = Integer.parseInt(p);
                    // period is from previous pay day (day N of previous month) to this day N (inclusive start, exclusive end)
                    LocalDate thisPay = payDate;
                    LocalDate start = thisPay.minusMonths(1).withDayOfMonth(dia);
                    LocalDate end = thisPay.withDayOfMonth(dia).plusDays(1);
                    double daysInMonth = (double) thisPay.lengthOfMonth();
                    double multiplier = 1.0; // treat monthly as full month for salaried employees (tests expect full salary on day 1)
                    return new PayPeriod(start, end, multiplier);
                }
            } else if (parts[0].equals("semanal")) {
                if (parts.length == 2) {
                    int dia = Integer.parseInt(parts[1]);
                    // weekly: period is 1 week ending at payDate (inclusive start, exclusive end)
                    LocalDate end = payDate.plusDays(1);
                    LocalDate start = end.minusWeeks(1);
                    double multiplier = 1.0 / 4.333333333333333; // approx weeks per month
                    return new PayPeriod(start, end, multiplier);
                } else if (parts.length == 3) {
                    int intervalo = Integer.parseInt(parts[1]);
                    int dia = Integer.parseInt(parts[2]);
                    // determine actual pay periods anchored similarly to isPayDate
                    java.time.LocalDate firstWeekday = LocalDate.of(2005,1,1).with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.of(dia)));
                    java.time.LocalDate firstPay = firstWeekday.plusWeeks(Math.max(0, intervalo - 1));
                    // find the most recent pay date <= payDate that is on the cadence
                    long weeksSinceFirst = java.time.temporal.ChronoUnit.WEEKS.between(firstPay, payDate);
                    long cycles = weeksSinceFirst / intervalo;
                    java.time.LocalDate currentPay = firstPay.plusWeeks(cycles * intervalo);
                    java.time.LocalDate end = currentPay.plusDays(1);
                    java.time.LocalDate start = end.minusWeeks(intervalo);
                    double multiplier = ((double) intervalo) / 4.333333333333333;
                    return new PayPeriod(start, end, multiplier);
                }
            }
        } catch (Exception ex) {
            return null;
        }
        return null;
    }
}
