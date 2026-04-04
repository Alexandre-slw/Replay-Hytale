package com.salwyrr.commands;

import com.salwyrr.ReplayPlugin;
import com.salwyrr.replay.ReplayPlayer;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;

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
        ReplayPlayer player = ReplayPlugin.get().getPlayer();

        if (runArg.get(context)) {
            player.start();
        } else {
            player.stop();
        }

        return CompletableFuture.completedFuture(null);
    }

}
