package com.alexandre.commands;

import com.hypixel.hytale.server.core.Message;
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
        context.sendMessage(Message.raw("Hello from ExampleCommand!"));
        return CompletableFuture.completedFuture(null);
    }

}
