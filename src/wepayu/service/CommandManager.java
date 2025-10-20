package wepayu.service;

import java.util.Map;
import java.util.Stack;

public class CommandManager {
    private static class CmdEntry {
        final Command cmd;
        final Map<String, wepayu.model.Employee> before;
        final Map<String, wepayu.model.Employee> after;
        final long beforeSeq;
        final long afterSeq;
        CmdEntry(Command cmd, Map<String, wepayu.model.Employee> before, long beforeSeq, Map<String, wepayu.model.Employee> after, long afterSeq) {
            this.cmd = cmd; this.before = before; this.beforeSeq = beforeSeq; this.after = after; this.afterSeq = afterSeq;
        }
    }

    private static Stack<CmdEntry> undoStack = new Stack<>();
    private static Stack<CmdEntry> redoStack = new Stack<>();
    // sequence counter to correlate commands with snapshots in logs
    private static long cmdSeqCounter = 0;

    public static void executeCommand(Command cmd) {
        System.out.println(String.format("TRACE_EXEC_CMD execute %s", cmd.getClass().getSimpleName()));
        // If the command is not undoable, just execute it and skip snapshotting/storing
        if (!cmd.isUndoable()) {
            System.out.println(String.format("TRACE_EXEC_CMD %s is not undoable; executing without snapshot", cmd.getClass().getSimpleName()));
            cmd.execute();
            // no change to undo/redo stacks
            return;
        }
        // capture snapshot before executing the command
        long beforeSeq = ++cmdSeqCounter;
        Map<String, wepayu.model.Employee> before = wepayu.service.PayrollDatabase.snapshotWithSeq(beforeSeq);
        // execute the command
        cmd.execute();
        // capture snapshot after executing the command
        long afterSeq = ++cmdSeqCounter;
        Map<String, wepayu.model.Employee> after = wepayu.service.PayrollDatabase.snapshotWithSeq(afterSeq);
        // push a combined entry so undo/redo are deterministic
        CmdEntry entry = new CmdEntry(cmd, before, beforeSeq, after, afterSeq);
        undoStack.push(entry);
        System.out.println(String.format("TRACE_CMD_SEQ pushed seq=%d cmd=%s", beforeSeq, cmd.getClass().getSimpleName()));
        // dump full stacks for diagnostics
        try {
            String dump = dumpStacksToString();
            System.out.println(dump);
            java.io.File d = new java.io.File("debug-snapshots"); if (!d.exists()) d.mkdirs();
            java.io.File f = new java.io.File(d, "stacks-after-exec-" + beforeSeq + ".txt");
            try (java.io.BufferedWriter w = new java.io.BufferedWriter(new java.io.FileWriter(f))) { w.write(dump); }
        } catch (Exception ex) { }
        // clear redo stack on new action
        redoStack.clear();
        // diagnostic: show stack sizes and top elements
        System.out.println(String.format("TRACE_STACKS after execute: undoSize=%d topUndo=%s redoSize=%d topRedo=%s",
            undoStack.size(), (undoStack.isEmpty() ? "-" : undoStack.peek().cmd.getClass().getSimpleName()),
            redoStack.size(), (redoStack.isEmpty() ? "-" : redoStack.peek().cmd.getClass().getSimpleName())));
    }

    public static void undo() {
        System.out.println("TRACE_EXEC_CMD undo called");
        // dump stacks before undo for clarity
        try { System.out.println(dumpStacksToString()); } catch (Exception ex) { }
        if (undoStack.isEmpty()) {
            throw new wepayu.service.exceptions.InvalidDataException("Nao ha comando a desfazer.");
        }
        CmdEntry entry = undoStack.pop();
        System.out.println(String.format("TRACE_EXEC_CMD undo %s", entry.cmd.getClass().getSimpleName()));
        // restore the DB to its previous state (before)
        wepayu.service.PayrollDatabase.restoreSnapshotWithSeq(entry.before, entry.beforeSeq);
        // push onto redo stack the same entry so redo can restore 'after'
        redoStack.push(entry);
        System.out.println(String.format("TRACE_CMD_SEQ undo pushed-to-redo seq=%d cmd=%s", entry.beforeSeq, entry.cmd.getClass().getSimpleName()));
        System.out.println(String.format("TRACE_STACKS after undo: undoSize=%d topUndo=%s redoSize=%d topRedo=%s",
                undoStack.size(), (undoStack.isEmpty() ? "-" : undoStack.peek().cmd.getClass().getSimpleName()),
                redoStack.size(), (redoStack.isEmpty() ? "-" : redoStack.peek().cmd.getClass().getSimpleName())));
        try {
            String dump = dumpStacksToString();
            java.io.File d = new java.io.File("debug-snapshots"); if (!d.exists()) d.mkdirs();
            java.io.File f = new java.io.File(d, "stacks-after-undo-" + entry.beforeSeq + ".txt");
            try (java.io.BufferedWriter w = new java.io.BufferedWriter(new java.io.FileWriter(f))) { w.write(dump); }
        } catch (Exception ex) { }
    }

    public static void redo() {
        System.out.println("TRACE_EXEC_CMD redo called");
        try { System.out.println(dumpStacksToString()); } catch (Exception ex) { }
        if (redoStack.isEmpty()) {
            throw new wepayu.service.exceptions.InvalidDataException("Nao ha comando a refazer.");
        }
        CmdEntry entry = redoStack.pop();
        System.out.println(String.format("TRACE_EXEC_CMD redo %s", entry.cmd.getClass().getSimpleName()));
        // restore DB to state after the original command executed
        wepayu.service.PayrollDatabase.restoreSnapshotWithSeq(entry.after, entry.afterSeq);
        // push onto undo stack the entry again
        undoStack.push(entry);
        System.out.println(String.format("TRACE_CMD_SEQ redo pushed-to-undo seq=%d cmd=%s", entry.beforeSeq, entry.cmd.getClass().getSimpleName()));
        System.out.println(String.format("TRACE_STACKS after redo: undoSize=%d topUndo=%s redoSize=%d topRedo=%s",
                undoStack.size(), (undoStack.isEmpty() ? "-" : undoStack.peek().cmd.getClass().getSimpleName()),
                redoStack.size(), (redoStack.isEmpty() ? "-" : redoStack.peek().cmd.getClass().getSimpleName())));
        try {
            String dump = dumpStacksToString();
            java.io.File d = new java.io.File("debug-snapshots"); if (!d.exists()) d.mkdirs();
            java.io.File f = new java.io.File(d, "stacks-after-redo-" + entry.beforeSeq + ".txt");
            try (java.io.BufferedWriter w = new java.io.BufferedWriter(new java.io.FileWriter(f))) { w.write(dump); }
        } catch (Exception ex) { }
    }

    private static String dumpStacksToString() {
        StringBuilder sb = new StringBuilder();
        sb.append("STACK_DUMP undoSize=").append(undoStack.size()).append(" redoSize=").append(redoStack.size()).append('\n');
        sb.append("UNDO_STACK:\n");
        for (int i = undoStack.size() - 1; i >= 0; --i) {
            CmdEntry e = undoStack.get(i);
            sb.append(String.format("  seq(before=%d,after=%d) cmd=%s\n", e.beforeSeq, e.afterSeq, e.cmd.getClass().getSimpleName()));
        }
        sb.append("REDO_STACK:\n");
        for (int i = redoStack.size() - 1; i >= 0; --i) {
            CmdEntry e = redoStack.get(i);
            sb.append(String.format("  seq(before=%d,after=%d) cmd=%s\n", e.beforeSeq, e.afterSeq, e.cmd.getClass().getSimpleName()));
        }
        return sb.toString();
    }

    public static void clear() {
        undoStack.clear();
        redoStack.clear();
    }
}
