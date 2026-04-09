package gg.alexandre.replay.commands;

import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import gg.alexandre.replay.ReplayPlugin;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

public class PlayCommand extends AbstractCommand {

    private final RequiredArg<Boolean> runArg;

    public PlayCommand(String name, String description) {
        super(name, description);

        runArg = withRequiredArg("run", "", ArgTypes.BOOLEAN);
    }

    @Nullable
    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
        if (runArg.get(context)) {
            ReplayPlugin.get().startReplaying();
        } else {
            ReplayPlugin.get().stopReplaying();
        }

        return CompletableFuture.completedFuture(null);
    }

}
