package gg.alexandre.replay.commands;

import com.hypixel.hytale.common.semver.Semver;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import gg.alexandre.replay.ReplayPlugin;
import gg.alexandre.replay.ui.CutSceneUI;
import gg.alexandre.replay.util.UpdateUtil;

import javax.annotation.Nonnull;
import java.awt.*;

public class CutSceneCommand extends AbstractPlayerCommand {

    public CutSceneCommand(@Nonnull String name, @Nonnull String description) {
        super(name, description);

        addAliases("cutscenes");
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    @Override
    protected void execute(@Nonnull CommandContext commandContext, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        Semver version = ReplayPlugin.get().getManifest().getVersion();
        UpdateUtil.getUpdateAsync(version).thenAccept(update -> {
            if (update.isEmpty()) {
                return;
            }

            commandContext.sendMessage(Message.join(
                    Message.translation("replay.updateAvailableCurseForge"),
                    Message.raw(" v" + update.get())
            ).color(Color.GREEN));
        });

        store.getExternalData().getWorld().execute(() -> {
            Player player = store.getComponent(ref, Player.getComponentType());
            assert player != null;

            if (ReplayPlugin.get().getReplayPlayer().isPlaying(playerRef)) {
                commandContext.sendMessage(Message.translation("replay.cannotDoThatWhileReplaying"));
                return;
            }

            player.getPageManager().openCustomPage(
                    ref,
                    store,
                    new CutSceneUI(
                            playerRef, ReplayPlugin.get().getCutSceneRepository(), ReplayPlugin.get().getRecorder()
                    )
            );
        });
    }

}
