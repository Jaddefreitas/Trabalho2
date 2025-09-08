package wepayu.service;

public interface Command {
    void execute();
    void undo();
}
