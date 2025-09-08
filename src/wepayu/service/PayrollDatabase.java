package wepayu.service;

import wepayu.model.Employee;
import java.util.HashMap;
import java.util.Map;

public class PayrollDatabase {
    private static Map<String, Employee> employees = new HashMap<>();

    public static void addEmployee(Employee e) {
        employees.put(e.getId(), e);
    }

    public static Employee getEmployee(String id) {
        return employees.get(id);
    }

    public static void removeEmployee(String id) {
        employees.remove(id);
    }

    public static Map<String, Employee> getAllEmployees() {
        return employees;
    }

    public static void clear() {
        employees.clear();
    }
}
