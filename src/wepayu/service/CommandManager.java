package wepayu.service;

import java.util.Stack;

public class CommandManager {
    private static Stack<Command> undoStack = new Stack<>();
    private static Stack<Command> redoStack = new Stack<>();

    public static void executeCommand(Command cmd) {
        cmd.execute();
        undoStack.push(cmd);
        redoStack.clear();
    }

    public static void undo() {
        if (!undoStack.isEmpty()) {
            Command cmd = undoStack.pop();
            cmd.undo();
            redoStack.push(cmd);
        }
    }

    public static void redo() {
        if (!redoStack.isEmpty()) {
            Command cmd = redoStack.pop();
            cmd.execute();
            undoStack.push(cmd);
        }
    }

    public static void clear() {
        undoStack.clear();
        redoStack.clear();
    }
}
