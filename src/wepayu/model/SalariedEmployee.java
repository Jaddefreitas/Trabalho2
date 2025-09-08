package wepayu.model;

import wepayu.util.DateUtils;

public class SalariedEmployee extends Employee {
    private double monthlySalary;

    public SalariedEmployee(String name, String address, double monthlySalary) {
        super(name, address);
        this.monthlySalary = monthlySalary;
        this.paymentSchedule = PaymentSchedule.MENSAL;
    }

    public double getMonthlySalary() {
        return monthlySalary;
    }

    @Override
    public double calculatePay() {
        double total = monthlySalary;
        if (unionMembership != null) {
            total -= unionMembership.getTotalCharges();
        }
        return total;
    }

    @Override
    public boolean isPayDate(String date) {
        return util.DateUtils.isLastWorkDayOfMonth(date);
    }
}

