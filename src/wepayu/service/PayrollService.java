package wepayu.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import wepayu.model.*;

public class PayrollService {

    private static final BigDecimal WEEKS_IN_MONTH = new BigDecimal("4.333333333333333");
    // Set to true to enable detailed payroll debug output. Keep false during automated tracing.
    private static final boolean ENABLE_PAYROLL_DEBUG = true;

    private static BigDecimal roundMoney(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    public static List<Paycheck> runPayroll(String date) {
        // Write a lightweight snapshot of the employee database for debugging
        try {
            writeEmployeeSnapshot(date);
        } catch (Exception ex) {
            // swallow any errors - this is only diagnostic
        }
        List<Paycheck> checks = new ArrayList<>();
        for (Employee e : PayrollDatabase.getAllEmployees().values()) {
            // Skip internal schedule placeholders which are stored as special Employee entries
            // with ids like "__SCHEDULE__::mensal $". They must not be considered for payroll.
            if (e.getId() != null && e.getId().startsWith("__SCHEDULE__::")) continue;
            boolean payDate = false;
            wepayu.util.ScheduleUtils.PayPeriod period = null;
            if (e.getPaymentScheduleDescription() != null && !e.getPaymentScheduleDescription().isBlank()) {
                payDate = wepayu.util.ScheduleUtils.isPayDateForDescriptor(e.getPaymentScheduleDescription(), date);
                if (payDate) {
                    period = wepayu.util.ScheduleUtils.getPayPeriodForDescriptor(e.getPaymentScheduleDescription(), date);
                }
            } else {
                payDate = e.isPayDate(date);
            }
            if (payDate) {
                // debug: report why this employee is considered payable on this date
                String scheduleDesc = e.getPaymentScheduleDescription();
                String sched = scheduleDesc == null ? (e.getPaymentSchedule() == null ? "<none>" : e.getPaymentSchedule().name()) : scheduleDesc;
        if (ENABLE_PAYROLL_DEBUG) {
            System.out.println(String.format("DEBUG_PAYDATE id=%s name=%s schedule=%s payDate=%s period=%s",
                e.getId(), e.getName(), sched, String.valueOf(payDate), (period == null ? "<all>" : String.valueOf(period.multiplier))));
        }
                // compute gross and deductions for the pay period (if available) or whole history if not
                BigDecimal gross = BigDecimal.ZERO;
                BigDecimal deductions = BigDecimal.ZERO;
                String startStr = null, endStr = null;
                if (period != null) {
                    startStr = period.start.getDayOfMonth() + "/" + period.start.getMonthValue() + "/" + period.start.getYear();
                    endStr = period.end.getDayOfMonth() + "/" + period.end.getMonthValue() + "/" + period.end.getYear();
                }

                // Salaried (non-commissioned)
                if (e instanceof wepayu.model.SalariedEmployee && !(e instanceof wepayu.model.CommissionedEmployee)) {
                    BigDecimal monthly = BigDecimal.valueOf(((wepayu.model.SalariedEmployee) e).getMonthlySalary());
                    if (period != null) {
                        String desc = e.getPaymentScheduleDescription() == null ? "" : e.getPaymentScheduleDescription().toLowerCase();
                        String[] descParts = desc.split("\\s+");
                        if (descParts.length >= 2 && descParts[0].equals("semanal")) {
                            // compute prorated monthly amount for the period (no per-week rounding)
                            BigDecimal raw = monthly.multiply(BigDecimal.valueOf(period.multiplier));
                            gross = gross.add(raw);
                        } else {
                            BigDecimal raw = monthly.multiply(BigDecimal.valueOf(period.multiplier));
                            gross = gross.add(raw);
                        }
                    } else {
                        gross = gross.add(monthly);
                    }
                } else if (e instanceof wepayu.model.CommissionedEmployee) {
                    // Commissioned: base monthly prorated + commissions
                    BigDecimal base = BigDecimal.valueOf(((wepayu.model.CommissionedEmployee) e).getMonthlySalary());
                    if (period != null) {
                        String desc = e.getPaymentScheduleDescription() == null ? "" : e.getPaymentScheduleDescription().toLowerCase();
                        String[] descParts = desc.split("\\s+");
                        if (descParts.length >= 2 && descParts[0].equals("semanal")) {
                            // prorate base monthly salary by period multiplier (no per-week rounding)
                            BigDecimal raw = base.multiply(BigDecimal.valueOf(period.multiplier));
                            gross = gross.add(raw);
                        } else {
                            BigDecimal raw = base.multiply(BigDecimal.valueOf(period.multiplier));
                            gross = gross.add(raw);
                        }
                    } else {
                        gross = gross.add(base);
                    }
                    // commissions: sum sales within period (high precision, do not round yet)
                    BigDecimal commissions = BigDecimal.ZERO;
                    double commissionRate = ((wepayu.model.CommissionedEmployee) e).getCommissionRate();
                    for (wepayu.model.SalesReceipt sr : ((wepayu.model.CommissionedEmployee) e).sales) {
                        if (period == null || wepayu.util.DateUtils.isBetweenExclusiveEnd(sr.getDate(), startStr, endStr)) {
                            BigDecimal amt = BigDecimal.valueOf(sr.getAmount());
                            commissions = commissions.add(amt.multiply(BigDecimal.valueOf(commissionRate)));
                        }
                    }
                    gross = gross.add(commissions);
                } else if (e instanceof wepayu.model.HourlyEmployee) {
                    // Hourly: compute hours in period
                    BigDecimal total = BigDecimal.ZERO;
                    if (period != null) {
                        double normalHours = ((wepayu.model.HourlyEmployee) e).getHorasTrabalhadas(startStr, endStr, false);
                        double extraHours = ((wepayu.model.HourlyEmployee) e).getHorasTrabalhadas(startStr, endStr, true);
                        BigDecimal rate = BigDecimal.valueOf(((wepayu.model.HourlyEmployee) e).getHourlyRate());
                        BigDecimal normal = BigDecimal.valueOf(normalHours).multiply(rate);
                        BigDecimal extra = BigDecimal.valueOf(extraHours).multiply(rate).multiply(BigDecimal.valueOf(1.5d));
                        total = total.add(normal).add(extra);
                    } else {
                        double calc = ((wepayu.model.HourlyEmployee) e).calculatePay();
                        total = BigDecimal.valueOf(calc);
                    }
                    gross = gross.add(total);
                }

                // deductions: union service charges in period + prorated monthly fee
                if (e.getUnionMembership() != null) {
                    if (period != null) {
                        double monthlyFee = e.getUnionMembership().getMonthlyFee();
                        String desc = e.getPaymentScheduleDescription() == null ? "" : e.getPaymentScheduleDescription().toLowerCase();
                        String[] descParts = desc.split("\\s+");
                        if (descParts.length >= 2 && descParts[0].equals("semanal")) {
                            // prorate monthly fee by period multiplier (no per-week rounding)
                            BigDecimal rawFee = BigDecimal.valueOf(monthlyFee).multiply(BigDecimal.valueOf(period.multiplier));
                            deductions = deductions.add(rawFee);
                        } else {
                            BigDecimal rawFee = BigDecimal.valueOf(monthlyFee).multiply(BigDecimal.valueOf(period.multiplier));
                            deductions = deductions.add(rawFee);
                        }
                        for (wepayu.model.ServiceCharge sc : e.getUnionMembership().getServiceCharges()) {
                            if (wepayu.util.DateUtils.isBetweenExclusiveEnd(sc.getDate(), startStr, endStr)) {
                                deductions = deductions.add(BigDecimal.valueOf(sc.getAmount()));
                            }
                        }
                    } else {
                        deductions = deductions.add(BigDecimal.valueOf(e.getUnionMembership().getTotalCharges()));
                    }
                }

        // prepare rounded values and print a debug line so we can compare computed triples
        // Do NOT round here: create paycheck with precise BigDecimal values.
        BigDecimal netUnrounded = gross.subtract(deductions);
        if (ENABLE_PAYROLL_DEBUG) {
            System.out.println(String.format("DEBUG_PAYROLL %s | %s | period=%s | start=%s | end=%s | grossRaw=%s | dedRaw=%s | netUnrounded=%s",
                e.getId(), e.getClass().getSimpleName(), (period == null ? "<all>" : String.valueOf(period.multiplier)), startStr, endStr,
                gross.toPlainString(), deductions.toPlainString(), netUnrounded.toPlainString()));
        }

    // Do NOT round gross/deductions here; keep precise values in the Paycheck.
    // Final per-employee net rounding (HALF_UP) is performed by callers when needed
    // (for display or aggregation). This avoids double-rounding differences.
    Paycheck pc = new Paycheck(e.getId(), gross, deductions);
        checks.add(pc);
            }
        }
        return checks;
    }

    private static void writeEmployeeSnapshot(String date) {
        Map<String, Employee> all = PayrollDatabase.getAllEmployees();
        File dir = new File("debug-snapshots");
        if (!dir.exists()) dir.mkdirs();
        String fname = "debug-snapshot-" + date.replace('/', '-') + "-" + System.currentTimeMillis() + ".txt";
        File out = new File(dir, fname);
        try (BufferedWriter w = new BufferedWriter(new FileWriter(out))) {
            w.write("Snapshot for date: " + date);
            w.newLine();
            for (Employee e : all.values()) {
                w.write(e.getId() + " | " + e.getName() + " | " + (e.getPaymentScheduleDescription() == null ? "" : e.getPaymentScheduleDescription()));
                w.newLine();
            }
        } catch (IOException ex) {
            // ignore
        }
    }
}
