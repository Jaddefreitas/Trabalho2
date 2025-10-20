package wepayu.service;

import java.util.LinkedHashMap;
import java.util.Map;
import wepayu.model.Employee;

public class PayrollDatabase {
    // Use LinkedHashMap to preserve insertion order (employees created order) which
    // is relied upon by tests/fixtures when reusing positional IDs in payroll outputs.
    private static Map<String, Employee> employees = new LinkedHashMap<>();

    public static void addEmployee(Employee e) {
        employees.put(e.getId(), e);
        // Log creation to disk for debugging id<->name mapping
        try {
            java.io.File d = new java.io.File("debug-snapshots"); if (!d.exists()) d.mkdirs();
            java.io.File f = new java.io.File(d, "creation-log.txt");
            try (java.io.BufferedWriter w = new java.io.BufferedWriter(new java.io.FileWriter(f, true))) {
                String line = System.currentTimeMillis() + " | " + e.getId() + " | " + e.getName() + " | " + (e.getPaymentScheduleDescription()==null?"":e.getPaymentScheduleDescription()) + " | " + e.getClass().getSimpleName();
                w.write(line);
                w.newLine();
            }
            System.out.println("TRACE_ADD_EMP: " + e.getId() + " -> " + e.getName());
        } catch (Exception ex) {
            // ignore logging failures
        }
    }

    public static Employee getEmployee(String id) {
        Employee e = employees.get(id);
        if (e == null) {
            // Log missing lookup with diagnostic snapshot to help correlate restores
            int sz = employees.size();
            System.out.println(String.format("TRACE_GET_EMP id=%s -> MISSING (current count=%d)", id, sz));
            try {
                java.io.File d = new java.io.File("debug-snapshots"); if (!d.exists()) d.mkdirs();
                String ts = String.valueOf(System.currentTimeMillis());
                java.io.File f = new java.io.File(d, "missing-" + id + "-" + ts + ".txt");
                try (java.io.BufferedWriter w = new java.io.BufferedWriter(new java.io.FileWriter(f))) {
                    w.write("missing-id=" + id); w.newLine();
                    w.write("timestamp=" + ts); w.newLine();
                    w.write("mapIdentityHash=" + System.identityHashCode(employees)); w.newLine();
                    w.write("mapSize=" + sz); w.newLine();
                    w.write("keys:"); w.newLine();
                    for (String k : employees.keySet()) { w.write(k); w.newLine(); }
                }
            } catch (Exception ex) {
                // ignore diagnostic write failures to avoid masking test results
            }
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
        // Create a deep-cloned snapshot to capture immutable state for undo/redo.
        // Shallow-copying references isn't sufficient because many commands mutate
        // Employee objects in-place (setName, setUnionMembership, addTimeCard, ...).
        // We therefore clone each Employee and its mutable sub-objects so the
        // snapshot is a true point-in-time capture.
        Map<String, Employee> snap = new LinkedHashMap<>();
        for (Map.Entry<String, Employee> e : employees.entrySet()) {
            snap.put(e.getKey(), deepCloneEmployee(e.getValue()));
        }
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

        // Deep-clone helpers
        private static Employee deepCloneEmployee(Employee src) {
            if (src == null) return null;
            try {
                Employee clone = null;
                if (src instanceof wepayu.model.CommissionedEmployee) {
                    wepayu.model.CommissionedEmployee c = (wepayu.model.CommissionedEmployee) src;
                    wepayu.model.CommissionedEmployee nc = new wepayu.model.CommissionedEmployee(c.getName(), c.getAddress(), ((wepayu.model.SalariedEmployee)c).getMonthlySalary(), c.getCommissionRate());
                    // reclaim the increment consumed by constructor
                    wepayu.model.Employee.decrementIdCounter();
                    // copy sales receipts
                    for (wepayu.model.SalesReceipt sr : c.sales) {
                        nc.addSalesReceipt(new wepayu.model.SalesReceipt(sr.getDate(), sr.getAmount()));
                    }
                    clone = nc;
                } else if (src instanceof wepayu.model.SalariedEmployee) {
                    wepayu.model.SalariedEmployee s = (wepayu.model.SalariedEmployee) src;
                    wepayu.model.SalariedEmployee ns = new wepayu.model.SalariedEmployee(s.getName(), s.getAddress(), s.getMonthlySalary());
                    wepayu.model.Employee.decrementIdCounter();
                    clone = ns;
                } else if (src instanceof wepayu.model.HourlyEmployee) {
                    wepayu.model.HourlyEmployee h = (wepayu.model.HourlyEmployee) src;
                    wepayu.model.HourlyEmployee nh = new wepayu.model.HourlyEmployee(h.getName(), h.getAddress(), h.getHourlyRate());
                    wepayu.model.Employee.decrementIdCounter();
                    // copy time cards
                    try {
                        java.lang.reflect.Field tcField = wepayu.model.HourlyEmployee.class.getDeclaredField("timeCards");
                        tcField.setAccessible(true);
                        java.util.List<wepayu.model.TimeCard> tcs = (java.util.List<wepayu.model.TimeCard>) tcField.get(h);
                        if (tcs != null) {
                            for (wepayu.model.TimeCard t : tcs) nh.addTimeCard(new wepayu.model.TimeCard(t.getDate(), t.getHours()));
                        }
                    } catch (Exception ex) {
                        // fallback: no timecards copied
                    }
                    clone = nh;
                } else {
                    // fallback: generic salaried clone
                    wepayu.model.SalariedEmployee generic = new wepayu.model.SalariedEmployee(src.getName(), src.getAddress(), 0);
                    wepayu.model.Employee.decrementIdCounter();
                    clone = generic;
                }

                // copy common fields
                clone.setId(src.getId());
                clone.setName(src.getName());
                clone.setAddress(src.getAddress());
                clone.setPaymentMethod(src.getPaymentMethod());
                clone.setPaymentSchedule(src.getPaymentSchedule());
                clone.setPaymentScheduleDescription(src.getPaymentScheduleDescription());
                clone.setBankName(getFieldSafe(src, "bankName"));
                clone.setAgency(getFieldSafe(src, "agency"));
                clone.setAccount(getFieldSafe(src, "account"));

                // copy union membership (including service charges)
                if (src.getUnionMembership() != null) {
                    wepayu.model.UnionMembership um = src.getUnionMembership();
                    wepayu.model.UnionMembership num = new wepayu.model.UnionMembership(um.getUnionId(), um.getMonthlyFee());
                    // copy service charges
                    for (wepayu.model.ServiceCharge sc : um.getServiceCharges()) {
                        num.addServiceCharge(new wepayu.model.ServiceCharge(sc.getDate(), sc.getAmount()));
                    }
                    clone.setUnionMembership(num);
                }

                return clone;
            } catch (Exception ex) {
                // On failure, fall back to shallow reference to avoid losing data
                return src;
            }
        }

        // Reflection helper to access private bank fields safely
        private static String getFieldSafe(Employee src, String fieldName) {
            try {
                java.lang.reflect.Field f = Employee.class.getDeclaredField(fieldName);
                f.setAccessible(true);
                Object v = f.get(src);
                return v == null ? null : v.toString();
            } catch (Exception ex) {
                return null;
            }
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
        // Perform an atomic swap of the internal employees map reference instead of
        // clearing and repopulating the existing map. This avoids transient states
        // where other code paths might observe a partially-populated map and
        // prevents subtle loss of data when Employee objects or placeholders are
        // reintroduced concurrently during the clear+putAll window.
        if (snapshot == null) {
            employees = new LinkedHashMap<>();
        } else {
            // preserve insertion order from snapshot
            employees = new LinkedHashMap<>(snapshot);
            // diagnostic: verify that the live map matches the snapshot
            try {
                java.util.List<String> snapKeys = new java.util.ArrayList<>(snapshot.keySet());
                java.util.List<String> liveKeys = new java.util.ArrayList<>(employees.keySet());
                System.out.println(String.format("TRACE_POST_RESTORE seq=%d snapKeys=%s liveKeys=%s", externalSeq, snapKeys.toString(), liveKeys.toString()));
                if (snapKeys.size() != liveKeys.size() || !snapKeys.equals(liveKeys)) {
                    System.out.println(String.format("TRACE_POST_RESTORE MISMATCH seq=%d snapSize=%d liveSize=%d", externalSeq, snapKeys.size(), liveKeys.size()));
                }
            } catch (Exception ex) {
                // ignore diagnostic errors
            }
        }
    }
}

class SnapshotSeq {
    private static java.util.concurrent.atomic.AtomicLong COUNTER = new java.util.concurrent.atomic.AtomicLong(0);
    public static long next() { return COUNTER.incrementAndGet(); }
}
