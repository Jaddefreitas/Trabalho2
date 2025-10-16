package wepayu;

import java.util.List;
import wepayu.model.Paycheck;
import wepayu.service.PayrollService;

public class DebugPayroll {
    public static void main(String[] args) {
        String date = "7/1/2005";
        if (args.length > 0) date = args[0];
        List<Paycheck> checks = PayrollService.runPayroll(date);
        double total = 0.0;
        System.out.println("Debug payroll for date: " + date);
        for (Paycheck pc : checks) {
            System.out.printf("id=%s gross=%.6f deductions=%.6f net=%.6f\n", pc.getEmployeeId(), pc.getGrossPay(), pc.getDeductions(), pc.getNetPay());
            total += pc.getNetPay();
        }
        System.out.printf("Total net: %.6f (count=%d)\n", total, checks.size());
    }
}
