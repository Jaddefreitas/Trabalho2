package wepayu.model;

import java.util.UUID;

public abstract class Employee {
    protected String id;
    protected String name;
    protected String address;
    protected PaymentMethod paymentMethod;
    protected PaymentSchedule paymentSchedule;
    protected UnionMembership unionMembership;

    public Employee(String name, String address) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.address = address;
        this.paymentMethod = PaymentMethod.CHEQUE_CORREIOS; // padrão
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
    public void setName(String new_name){
        this.name = new_name;
    }

    public String getAddress() {
        return address;
    }
    public void setAddress(String new_adress){
        this.address = new_adress;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public UnionMembership getUnionMembership() {
        return unionMembership;
    }

    public void setUnionMembership(UnionMembership unionMembership) {
        this.unionMembership = unionMembership;
    }

    public PaymentSchedule getPaymentSchedule() {
        return paymentSchedule;
    }

    public void setPaymentSchedule(PaymentSchedule paymentSchedule) {
        this.paymentSchedule = paymentSchedule;
    }

    public abstract double calculatePay();

    public abstract boolean isPayDate(String date);

}
