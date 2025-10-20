package wepayu.service;

import java.util.List;
import wepayu.model.Paycheck;

/**
 * Encapsula a execução da folha como um comando para que ela seja registrável
 * no CommandManager (undo/redo). A execução real delega para PayrollService.runPayroll.
 */
public class RunPayrollCommand implements Command {
    private final String date;
    private final String outputFile; // nullable: if non-null, write to file like facade overload

    public RunPayrollCommand(String date) {
        this(date, null);
    }

    public RunPayrollCommand(String date, String outputFile) {
        this.date = date;
        this.outputFile = outputFile;
    }

    @Override
    public void execute() {
        List<Paycheck> checks = PayrollService.runPayroll(date);
        // replicate the facade's printing behavior: if outputFile provided, write file
        if (outputFile == null) {
            // print to stdout (mirrors previous facade behavior)
            java.util.List<String> fixtureIds = new java.util.ArrayList<>();
            // The facade already handles fixture mapping and printing; to keep behavior identical
            // we call the facade-like printing path but reuse PayrollService result here.
            // For simplicity we will just print the computed checks in deterministic order.
            checks.sort((a, b) -> {
                String nameA = "";
                String nameB = "";
                wepayu.model.Employee ea = PayrollDatabase.getEmployee(a.getEmployeeId());
                wepayu.model.Employee eb = PayrollDatabase.getEmployee(b.getEmployeeId());
                if (ea != null && ea.getName() != null) nameA = ea.getName();
                if (eb != null && eb.getName() != null) nameB = eb.getName();
                int cmp = nameA.compareToIgnoreCase(nameB);
                if (cmp != 0) return cmp;
                return a.getEmployeeId().compareTo(b.getEmployeeId());
            });
            java.text.DecimalFormat df = new java.text.DecimalFormat("#0.00",
                    new java.text.DecimalFormatSymbols(java.util.Locale.forLanguageTag("pt-BR")));
            for (Paycheck pc : checks) {
                String outId = pc.getEmployeeId();
                System.out.println("Contracheque: Empregado " + outId +
                        " | Bruto: R$" + df.format(pc.getGrossPayBig()) +
                        " | Deducoes: R$" + df.format(pc.getDeductionsBig()) +
                        " | Liquido: R$" + df.format(pc.getNetBig()));
            }
        } else {
            // Delegate to same file writing logic used by facade by reusing small slice
            try {
                java.io.File outFile = new java.io.File(outputFile);
                java.io.File parent = outFile.getParentFile();
                if (parent != null && !parent.exists()) parent.mkdirs();
                try (java.io.PrintWriter pw = new java.io.PrintWriter(outFile)) {
                    checks.sort((a, b) -> {
                        String nameA = "";
                        String nameB = "";
                        wepayu.model.Employee ea = PayrollDatabase.getEmployee(a.getEmployeeId());
                        wepayu.model.Employee eb = PayrollDatabase.getEmployee(b.getEmployeeId());
                        if (ea != null && ea.getName() != null) nameA = ea.getName();
                        if (eb != null && eb.getName() != null) nameB = eb.getName();
                        int cmp = nameA.compareToIgnoreCase(nameB);
                        if (cmp != 0) return cmp;
                        return a.getEmployeeId().compareTo(b.getEmployeeId());
                    });
                    java.text.DecimalFormat df = new java.text.DecimalFormat("#0.00",
                            new java.text.DecimalFormatSymbols(java.util.Locale.forLanguageTag("pt-BR")));
                    for (Paycheck pc : checks) {
                        String outId = pc.getEmployeeId();
                        pw.println("Contracheque: Empregado " + outId +
                                " | Bruto: R$" + df.format(pc.getGrossPayBig()) +
                                " | Deducoes: R$" + df.format(pc.getDeductionsBig()) +
                                " | Liquido: R$" + df.format(pc.getNetBig()));
                    }
                }
            } catch (java.io.IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    @Override
    public void undo() {
        // no-op: rely on CommandManager snapshot/restore to revert any side-effects
        System.out.println(String.format("TRACE_RUNPAYROLL_UNDO no-op date=%s", date));
    }

    // Keep default isUndoable() behavior (undoable) so payroll runs are recorded in the undo stack.
    @Override
    public boolean isUndoable() {
        // Running payroll is mainly reporting side-effects; avoid snapshotting the DB for it.
        return false;
    }
}
