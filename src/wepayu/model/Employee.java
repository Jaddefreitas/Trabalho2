package wepayu.model;

import java.util.concurrent.atomic.AtomicLong;

public abstract class Employee {
    protected String id;
    private static final AtomicLong ID_COUNTER = new AtomicLong(1);
    protected String name;
    protected String address;
    protected PaymentMethod paymentMethod;
    protected PaymentSchedule paymentSchedule;
    protected UnionMembership unionMembership;

    public Employee(String name, String address) {
    // deterministic id for reproducible runs: simple sequential id EMP-1, EMP-2, ...
    long seq = ID_COUNTER.getAndIncrement();
    this.id = "EMP-" + seq;
        this.name = name;
        this.address = address;
        this.paymentMethod = PaymentMethod.CHEQUE_MAOS; // padrão
    }

    /**
     * Decrement the internal ID counter by one.
     * This is used to "reclaim" an increment consumed by transient placeholder objects
     * so they don't shift the EMP-N sequence used by tests.
     */
    public static void decrementIdCounter() {
        ID_COUNTER.decrementAndGet();
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

    // Bank/payment details (optional)
    private String bankName;
    private String agency;
    private String account;

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getAgency() {
        return agency;
    }

    public void setAgency(String agency) {
        this.agency = agency;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    // Allow preserving id when replacing employee types
    public void setId(String id) {
        this.id = id;
    }

    public PaymentSchedule getPaymentSchedule() {
        return paymentSchedule;
    }

    public void setPaymentSchedule(PaymentSchedule paymentSchedule) {
        this.paymentSchedule = paymentSchedule;
    }

    // Human-readable payment schedule descriptor (ex: "mensal $", "semanal 5", "semanal 2 5")
    private String paymentScheduleDescription;

    public String getPaymentScheduleDescription() {
        return paymentScheduleDescription;
    }

    public void setPaymentScheduleDescription(String paymentScheduleDescription) {
        this.paymentScheduleDescription = paymentScheduleDescription;
    }

    public abstract double calculatePay();

    public abstract boolean isPayDate(String date);

}
