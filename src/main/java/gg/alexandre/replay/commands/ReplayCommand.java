package gg.alexandre.replay.commands;

import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

public class ReplayCommand extends AbstractCommand {

    public ReplayCommand(String name, String description) {
        super(name, description);

        addSubCommand(new RecordCommand("record", ""));
        addSubCommand(new PlayCommand("play", ""));
    }

    @Nullable
    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
        return CompletableFuture.completedFuture(null);
    }

}
