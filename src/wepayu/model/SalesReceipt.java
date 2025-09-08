package wepayu.model;

public class SalesReceipt {
    private String date;
    private double amount;

    public SalesReceipt(String date, double amount) {
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
