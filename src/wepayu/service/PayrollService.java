package wepayu.service;

import wepayu.model.*;

import java.util.ArrayList;
import java.util.List;

public class PayrollService {

    public static List<Paycheck> runPayroll(String date) {
        List<Paycheck> checks = new ArrayList<>();
        for (Employee e : PayrollDatabase.getAllEmployees().values()) {
            if (e.isPayDate(date)) {
                double gross = e.calculatePay();
                double deductions = e.getUnionMembership() != null
                        ? e.getUnionMembership().getTotalCharges()
                        : 0;
                Paycheck pc = new Paycheck(e.getId(), gross, deductions);
                checks.add(pc);
                // Simulação: aqui você enviaria o pagamento via método escolhido
                System.out.println("Pagando " + e.getName() + " (" + e.getPaymentMethod() + ") -> R$ " + pc.getNetPay());
            }
        }
        return checks;
    }
}
