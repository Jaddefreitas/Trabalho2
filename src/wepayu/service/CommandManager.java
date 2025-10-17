package wepayu.service;

import java.util.Stack;
import java.util.Map;

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
        // clear redo stack on new action
        redoStack.clear();
        // diagnostic: show stack sizes and top elements
        System.out.println(String.format("TRACE_STACKS after execute: undoSize=%d topUndo=%s redoSize=%d topRedo=%s",
            undoStack.size(), (undoStack.isEmpty() ? "-" : undoStack.peek().cmd.getClass().getSimpleName()),
            redoStack.size(), (redoStack.isEmpty() ? "-" : redoStack.peek().cmd.getClass().getSimpleName())));
    }

    public static void undo() {
        System.out.println("TRACE_EXEC_CMD undo called");
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
    }

    public static void redo() {
        System.out.println("TRACE_EXEC_CMD redo called");
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
    }

    public static void clear() {
        undoStack.clear();
        redoStack.clear();
    }
}
