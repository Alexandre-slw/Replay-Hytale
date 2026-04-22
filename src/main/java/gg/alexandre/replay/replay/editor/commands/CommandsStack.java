package gg.alexandre.replay.replay.editor.commands;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class CommandsStack {

    private static final int MAX_SIZE = 100;

    private final List<CommandBase> commands = new ArrayList<>();
    private final List<CommandBase> redoCommands = new ArrayList<>();

    public void execute(CommandBase command) {
        execute(command, false);
    }

    private void execute(@Nonnull CommandBase command, boolean isRedo) {
        command.execute();

        if (!isRedo) {
            redoCommands.clear();

            if (!commands.isEmpty()) {
                CommandBase lastCommand = commands.getLast();
                if (lastCommand.canMerge(command) && lastCommand.merge(command)) {
                    return;
                }
            }
        }

        commands.add(command);

        while (commands.size() > MAX_SIZE) {
            commands.removeFirst();
        }
    }

    public void undo() {
        if (commands.isEmpty()) {
            return;
        }

        CommandBase command = commands.removeLast();
        command.undo();
        redoCommands.add(command);
    }

    public void redo() {
        if (redoCommands.isEmpty()) {
            return;
        }

        CommandBase command = redoCommands.removeLast();
        execute(command, true);
    }

    public boolean canUndo() {
        return !commands.isEmpty();
    }

    public boolean canRedo() {
        return !redoCommands.isEmpty();
    }

    public void clear() {}

}
