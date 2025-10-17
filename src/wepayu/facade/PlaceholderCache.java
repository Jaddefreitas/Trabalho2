package wepayu.facade;

import wepayu.model.Employee;
import wepayu.model.SalariedEmployee;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlaceholderCache {
    private static final Map<String, Employee> cache = new ConcurrentHashMap<>();

    public static Employee getPlaceholder(String key, String scheduleDesc) {
        return cache.computeIfAbsent(key, k -> {
            // Construct a temporary SalariedEmployee which would normally consume an EMP-N id.
            // Immediately decrement the global id counter to avoid shifting real employee ids
            // used by the tests, then set the id to the schedule key.
            Employee p = new SalariedEmployee("SCHEDULE", "", 0);
            // reclaim the incremented EMP counter so placeholders are id-less for tests
            wepayu.model.Employee.decrementIdCounter();
            p.setId(k);
            p.setPaymentScheduleDescription(scheduleDesc);
            return p;
        });
    }
}
