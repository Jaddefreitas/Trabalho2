package wepayu.service;

import wepayu.model.Employee;

public class RemoveEmployeeCommand implements Command {
    private String employeeId;
    private Employee backup;

    public RemoveEmployeeCommand(String employeeId) {
        this.employeeId = employeeId;
    }

    @Override
    public void execute() {
        backup = PayrollDatabase.getEmployee(employeeId);
        PayrollDatabase.removeEmployee(employeeId);
    }

    @Override
    public void undo() {
        if (backup != null) {
            PayrollDatabase.addEmployee(backup);
        }
    }
}
