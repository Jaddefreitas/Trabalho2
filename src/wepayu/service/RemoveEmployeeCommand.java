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
        System.out.println(String.format("TRACE_REMOVE_EMP id=%s present=%s", employeeId, (backup != null)));
    }

    @Override
    public void undo() {
        // No-op: rely on CommandManager's snapshot/restore to revert removals.
        System.out.println(String.format("TRACE_REMOVE_UNDO no-op id=%s", employeeId));
    }

    @Override
    public boolean isUndoable() {
        // Make removals non-undoable so undos don't reintroduce previously removed employees
        return false;
    }
}
