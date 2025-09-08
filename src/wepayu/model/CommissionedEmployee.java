package wepayu.model;

import java.util.ArrayList;
import java.util.List;
import wepayu.util.DateUtils;

public class CommissionedEmployee extends SalariedEmployee {
    private double commissionRate;
    private List<SalesReceipt> sales = new ArrayList<>();

    public CommissionedEmployee(String name, String address, double salary, double commissionRate) {
        super(name, address, salary);
        this.commissionRate = commissionRate;
        this.paymentSchedule = PaymentSchedule.BISEMANAL;
    }

    public void addSalesReceipt(SalesReceipt receipt) {
        sales.add(receipt);
    }

    public double getCommissionRate() {
        return commissionRate;
    }

    @Override
    public double calculatePay() {
        double basePay = super.calculatePay();
        double commissions = sales.stream()
                                  .mapToDouble(s -> s.getAmount() * commissionRate)
                                  .sum();
        return basePay + commissions;
    }

    @Override
    public boolean isPayDate(String date) {
        return util.DateUtils.isBiweeklyFriday(date);
    }
}
