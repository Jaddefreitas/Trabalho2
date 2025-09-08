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
    }

    @Override
    public void undo() {
        PayrollDatabase.removeEmployee(employee.getId());
    }
}
