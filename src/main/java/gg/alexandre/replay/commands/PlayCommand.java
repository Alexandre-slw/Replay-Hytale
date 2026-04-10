package gg.alexandre.replay.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
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
        Ref<EntityStore> ref = context.senderAsPlayerRef();
        Store<EntityStore> store = ref.getStore();
        store.getExternalData().getWorld().execute(() -> {
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

            if (runArg.get(context)) {
                // TODO: replay name
                ReplayPlugin.get().startReplaying(playerRef, "Apr 10, 2026, 3-16-32 PM");
            } else {
                ReplayPlugin.get().stopReplaying(playerRef);
            }
        });

        return CompletableFuture.completedFuture(null);
    }

}
