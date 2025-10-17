package wepayu.service;

import wepayu.model.Employee;
import java.util.Map;
import java.util.LinkedHashMap;

public class PayrollDatabase {
    // Use LinkedHashMap to preserve insertion order (employees created order) which
    // is relied upon by tests/fixtures when reusing positional IDs in payroll outputs.
    private static Map<String, Employee> employees = new LinkedHashMap<>();

    public static void addEmployee(Employee e) {
        employees.put(e.getId(), e);
    }

    public static Employee getEmployee(String id) {
        Employee e = employees.get(id);
        if (e == null) {
            System.out.println(String.format("TRACE_GET_EMP id=%s -> MISSING (current count=%d)", id, employees.size()));
        } else {
            System.out.println(String.format("TRACE_GET_EMP id=%s -> FOUND name=%s", id, e.getName()));
        }
        return e;
    }

    public static void removeEmployee(String id) {
        employees.remove(id);
    }

    public static Map<String, Employee> getAllEmployees() {
        return employees;
    }

    public static void clear() {
        System.out.println("TRACE_CLEAR_DATABASE: removing all employees (keeping schedule placeholders will be re-added)");
        employees.clear();
    }

    // Return a shallow snapshot copy of the current employees preserving insertion order
    public static Map<String, Employee> snapshot() {
        // default behavior: record a snapshot and generate an internal seq
        long seq = SnapshotSeq.next();
        return snapshotWithSeq(seq);
    }

    // Snapshot with externally supplied sequence number (used by CommandManager for correlation)
    public static Map<String, Employee> snapshotWithSeq(long externalSeq) {
        Map<String, Employee> snap = new LinkedHashMap<>(employees);
        // debug: write a numbered snapshot summary using the external seq
        try {
            java.io.File d = new java.io.File("debug-snapshots"); if (!d.exists()) d.mkdirs();
            java.io.File f = new java.io.File(d, "snap-" + externalSeq + "-exec.txt");
            try (java.io.BufferedWriter w = new java.io.BufferedWriter(new java.io.FileWriter(f))) {
                w.write("snapshot-seq=" + externalSeq + " size=" + snap.size()); w.newLine();
                for (String k : snap.keySet()) { w.write(k); w.newLine(); }
            }
            System.out.println(String.format("TRACE_SNAPSHOT seq=%d size=%d", externalSeq, snap.size()));
        } catch (Exception ex) { }
        return snap;
    }

    // Restore employees from a snapshot, replacing the current contents atomically
    // Restore employees from a snapshot, replacing the current contents atomically
    // Default variant uses an internally generated seq for debug file naming
    public static void restoreSnapshot(Map<String, Employee> snapshot) {
        long seq = SnapshotSeq.next();
        restoreSnapshotWithSeq(snapshot, seq);
    }

    // Restore with externally supplied sequence number (used by CommandManager for correlation)
    public static void restoreSnapshotWithSeq(Map<String, Employee> snapshot, long externalSeq) {
        try {
            java.io.File d = new java.io.File("debug-snapshots"); if (!d.exists()) d.mkdirs();
            java.io.File f = new java.io.File(d, "snap-" + externalSeq + "-restore.txt");
            try (java.io.BufferedWriter w = new java.io.BufferedWriter(new java.io.FileWriter(f))) {
                w.write("restore-seq=" + externalSeq + " restore-size=" + (snapshot == null ? 0 : snapshot.size())); w.newLine();
                if (snapshot != null) { for (String k : snapshot.keySet()) { w.write(k); w.newLine(); } }
            }
            System.out.println(String.format("TRACE_RESTORE seq=%d restoreSize=%d", externalSeq, (snapshot == null ? 0 : snapshot.size())));
        } catch (Exception ex) { }
        employees.clear();
        if (snapshot != null) {
            // preserve insertion order from snapshot
            employees.putAll(snapshot);
        }
    }
}

class SnapshotSeq {
    private static java.util.concurrent.atomic.AtomicLong COUNTER = new java.util.concurrent.atomic.AtomicLong(0);
    public static long next() { return COUNTER.incrementAndGet(); }
}
