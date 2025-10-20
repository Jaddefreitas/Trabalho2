package wepayu.service;

public interface Command {
    void execute();
    void undo();
    /**
     * Whether this command should be recorded for undo/redo. Default true.
     * Commands that are purely informational or produce non-reversible side-effects
     * can return false to avoid snapshotting the entire DB state.
     */
    default boolean isUndoable() { return true; }
}
