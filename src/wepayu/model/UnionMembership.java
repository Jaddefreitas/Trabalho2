package wepayu.model;

import java.util.ArrayList;
import java.util.List;

public class UnionMembership {
    private String unionId;
    private double monthlyFee;
    private List<ServiceCharge> serviceCharges = new ArrayList<>();

    public UnionMembership(String unionId, double monthlyFee) {
        this.unionId = unionId;
        this.monthlyFee = monthlyFee;
    }

    public void addServiceCharge(ServiceCharge charge) {
        serviceCharges.add(charge);
    }

    public double getTotalCharges() {
        double total = monthlyFee;
        for (ServiceCharge sc : serviceCharges) {
            total += sc.getAmount();
        }
        return total;
    }
}
