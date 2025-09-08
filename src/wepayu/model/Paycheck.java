package wepayu.model;

public class Paycheck {
    private String employeeId;
    private double grossPay;
    private double deductions;
    private double netPay;

    public Paycheck(String employeeId, double grossPay, double deductions) {
        this.employeeId = employeeId;
        this.grossPay = grossPay;
        this.deductions = deductions;
        this.netPay = grossPay - deductions;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public double getGrossPay() {
        return grossPay;
    }

    public double getDeductions() {
        return deductions;
    }

    public double getNetPay() {
        return netPay;
    }
}
