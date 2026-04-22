package gg.alexandre.replay.replay.editor.commands;

import javax.annotation.Nonnull;
import java.time.Instant;

public abstract class CommandBase {

    private final String id;
    private final Instant timestamp = Instant.now();

    public CommandBase(@Nonnull String id) {
        this.id = id;
    }

    public abstract void execute();

    public abstract void undo();

    public boolean merge(@Nonnull CommandBase other) {
        return false;
    }

    public boolean canMerge(@Nonnull CommandBase other) {
        return id.equals(other.id) && timestamp.plusSeconds(2).isAfter(other.timestamp);
    }

    @Nonnull
    public String getId() {
        return id;
    }

}
