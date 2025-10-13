package wepayu.model;

import java.util.ArrayList;
import java.util.List;
import wepayu.util.DateUtils;

public class HourlyEmployee extends Employee {
    private double hourlyRate;
    private List<TimeCard> timeCards = new ArrayList<>();

    public HourlyEmployee(String name, String address, double hourlyRate) {
        super(name, address);
        this.hourlyRate = hourlyRate;
        this.paymentSchedule = PaymentSchedule.SEMANAL;
    }

    public void addTimeCard(TimeCard card) {
        timeCards.add(card);
    }

    public double getHourlyRate() {
        return hourlyRate;
    }

    @Override
    public double calculatePay() {
        double total = 0;
        for (TimeCard card : timeCards) {
            double hours = card.getHours();
            if (hours > 8) {
                total += 8 * hourlyRate + (hours - 8) * hourlyRate * 1.5;
            } else {
                total += hours * hourlyRate;
            }
        }
        if (unionMembership != null) {
            total -= unionMembership.getTotalCharges();
        }
        return total;
    }

    @Override
    public boolean isPayDate(String date) {
        return DateUtils.isFriday(date);
    }

    public double getHorasTrabalhadas(String dataInicial, String dataFinal, boolean extras) {
    double total = 0;
    for (TimeCard c : timeCards) {
        // Agora: inclui o dia inicial, mas não o final
        if (DateUtils.isBetweenExclusiveEnd(c.getDate(), dataInicial, dataFinal)) {
            if (extras) {
                if (c.getHours() > 8) total += (c.getHours() - 8);
            } else {
                total += Math.min(c.getHours(), 8);
            }
        }
    }
    return total;
    }


}
