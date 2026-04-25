package gg.alexandre.replay.commands;

import com.hypixel.hytale.common.semver.Semver;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import gg.alexandre.replay.ReplayPlugin;
import gg.alexandre.replay.ui.ReplayUI;
import gg.alexandre.replay.util.UpdateUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.util.concurrent.CompletableFuture;

public class ReplayCommand extends AbstractCommand {

    public ReplayCommand(@Nullable String name, @Nullable String description) {
        super(name, description);
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    @Nullable
    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
        if (!context.isPlayer()) {
            context.sendMessage(Message.translation("replay.commandCanOnlyBeUsedByPlayers"));
            return CompletableFuture.completedFuture(null);
        }

        Semver version = ReplayPlugin.get().getManifest().getVersion();
        UpdateUtil.getUpdateAsync(version).thenAccept(update -> {
            if (update.isEmpty()) {
                return;
            }

            context.sendMessage(Message.join(
                    Message.translation("replay.updateAvailableCurseForge"),
                    Message.raw(" v" + update.get())
            ).color(Color.GREEN));
        });

        Player player = context.senderAs(Player.class);

        Ref<EntityStore> ref = player.getReference();
        assert ref != null;
        Store<EntityStore> store = ref.getStore();

        store.getExternalData().getWorld().execute(() -> {
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            assert playerRef != null;

            if (ReplayPlugin.get().getPlayer().isPlaying(playerRef)) {
                context.sendMessage(Message.translation("replay.cannotDoThatWhileReplaying"));
                return;
            }

            player.getPageManager().openCustomPage(
                    ref,
                    store,
                    new ReplayUI(playerRef, ReplayPlugin.get().getRepository(), ReplayPlugin.get().getRecorder())
            );
        });

        return CompletableFuture.completedFuture(null);
    }

}
