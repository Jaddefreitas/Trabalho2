package wepayu.service;

import wepayu.model.Employee;
import wepayu.facade.PlaceholderCache;

/**
 * Command that clears the system but is undoable by keeping a shallow backup of employees.
 */
public class ClearSystemCommand implements Command {
    @Override
    public void execute() {
        // Clear the database; snapshot/restore is handled by CommandManager
        // clear all non-placeholder employees and then re-add schedule placeholders
        System.out.println("TRACE_CLEAR_DATABASE: removing all employees (keeping schedule placeholders will be re-added)");
        PayrollDatabase.clear();
        // re-add default schedule placeholders here so the command's after-snapshot
        // will include them (ensures undo/redo restore consistent state)
        String[] defaults = new String[]{"mensal $", "semanal 5", "semanal 2 5"};
        for (String d : defaults) {
            String key = "__SCHEDULE__::" + d.toLowerCase();
            if (PayrollDatabase.getEmployee(key) == null) {
                Employee placeholder = PlaceholderCache.getPlaceholder(key, d);
                PayrollDatabase.addEmployee(placeholder);
            }
        }
    }

    @Override
    public void undo() {
        // No-op: CommandManager will restore the database snapshot on undo
    }
}
