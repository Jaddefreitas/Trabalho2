package wepayu.model;

public class ServiceCharge {
    private String date;
    private double amount;

    public ServiceCharge(String date, double amount) {
        this.date = date;
        this.amount = amount;
    }

    public String getDate() {
        return date;
    }

    public double getAmount() {
        return amount;
    }
}
