package wepayu.service;

import java.util.HashMap;
import java.util.Map;
import wepayu.model.Employee;

/**
 * Command that clears the system but is undoable by keeping a shallow backup of employees.
 */
public class ClearSystemCommand implements Command {
    private Map<String, Employee> backup;

    @Override
    public void execute() {
        // take a shallow copy of current employees
        backup = new HashMap<>(PayrollDatabase.getAllEmployees());
        // diagnostic
        try {
            java.io.File d = new java.io.File("debug-snapshots"); if (!d.exists()) d.mkdirs();
            java.io.File f = new java.io.File(d, "clear-exec-" + System.currentTimeMillis() + ".txt");
            try (java.io.BufferedWriter w = new java.io.BufferedWriter(new java.io.FileWriter(f))) {
                w.write("execute: backup-size=" + (backup == null ? 0 : backup.size())); w.newLine();
            }
        } catch (Exception ex) { }
        PayrollDatabase.clear();
    }

    @Override
    public void undo() {
        PayrollDatabase.clear();
        if (backup != null) {
            for (Employee e : backup.values()) PayrollDatabase.addEmployee(e);
        }
        try {
            java.io.File d = new java.io.File("debug-snapshots"); if (!d.exists()) d.mkdirs();
            java.io.File f = new java.io.File(d, "clear-undo-" + System.currentTimeMillis() + ".txt");
            try (java.io.BufferedWriter w = new java.io.BufferedWriter(new java.io.FileWriter(f))) {
                w.write("undo: restored-size=" + (backup == null ? 0 : backup.size())); w.newLine();
            }
        } catch (Exception ex) { }
    }
}
