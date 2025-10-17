package wepayu.service;

import wepayu.model.Employee;

public class AddEmployeeCommand implements Command {
    private Employee employee;

    public AddEmployeeCommand(Employee employee) {
        this.employee = employee;
    }

    @Override
    public void execute() {
        PayrollDatabase.addEmployee(employee);
        // lightweight trace for debugging test scripts (will be noisy; removed later)
        System.out.println(String.format("TRACE_ADD_EMP id=%s name=%s type=%s", employee.getId(), employee.getName(), employee.getClass().getSimpleName()));
    }

    @Override
    public void undo() {
        // No-op: rely on CommandManager's snapshot/restore to revert adds.
        System.out.println(String.format("TRACE_ADD_UNDO no-op id=%s", employee.getId()));
    }
}
