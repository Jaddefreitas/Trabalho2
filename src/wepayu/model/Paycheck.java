package wepayu.model;

import java.math.BigDecimal;

public class Paycheck {
    private String employeeId;
    // store monetary values as BigDecimal precisely; callers can format/round as needed
    private BigDecimal grossPay;
    private BigDecimal deductions;

    public Paycheck(String employeeId, BigDecimal grossPay, BigDecimal deductions) {
        this.employeeId = employeeId;
        this.grossPay = grossPay == null ? BigDecimal.ZERO : grossPay;
        this.deductions = deductions == null ? BigDecimal.ZERO : deductions;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    // backward-compatible double accessors (may be used by other code)
    public double getGrossPay() {
        return grossPay.doubleValue();
    }

    public double getDeductions() {
        return deductions.doubleValue();
    }

    // precise accessors
    public BigDecimal getGrossPayBig() {
        return grossPay;
    }

    public BigDecimal getDeductionsBig() {
        return deductions;
    }

    // computed net (not rounded) as BigDecimal
    public BigDecimal getNetBig() {
        return grossPay.subtract(deductions);
    }

    // backward-compatible net accessor (double)
    public double getNetPay() {
        return getNetBig().doubleValue();
    }
}
