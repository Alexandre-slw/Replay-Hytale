package gg.alexandre.replay.replay;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.tick.TickingSystem;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.packets.interaction.CancelInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import gg.alexandre.replay.replay.editor.commands.SetKeyframeValueCommand;
import gg.alexandre.replay.replay.editor.properties.CameraProperty;
import gg.alexandre.replay.replay.state.ReplayState;
import gg.alexandre.replay.ui.editor.EditorUI;
import gg.alexandre.replay.ui.manager.RealtimePageManager;
import gg.alexandre.replay.util.Position;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class BasePlayer extends TickingSystem<EntityStore> {

    protected final Map<UUID, ReplayState> states = new ConcurrentHashMap<>();

    public void restart(@Nonnull ReplayState state) {
        state.currentTick = 0;
        state.stage.isPlaying = false;
    }

    public void bypassFilter(@Nonnull ReplayState state, @Nonnull Runnable runnable) {

    }

    @Nullable
    protected ReplayState getState(@Nonnull PacketHandler packetHandler) {
        if (packetHandler.getAuth() == null) {
            return null;
        }

        return states.get(packetHandler.getAuth().getUuid());
    }

    protected void handleInteractionChains(@Nonnull PacketHandler handler, @Nonnull ReplayState state,
                                           @Nonnull SyncInteractionChains syncInteractionChains) {
        SyncInteractionChain interactionChain = null;

        for (int i = 0; i < syncInteractionChains.updates.length; i++) {
            SyncInteractionChain chain = syncInteractionChains.updates[i];
            if (chain.interactionType == InteractionType.Primary && chain.initial) {
                interactionChain = chain;
                break;
            }
        }

        if (interactionChain == null) {
            return;
        }

        SyncInteractionChain chain = interactionChain;
        bypassFilter(state, () ->
                handler.writeNoCache(new CancelInteractionChain(chain.chainId, chain.forkedId))
        );
        state.ui.controlGame = false;

        if (state.ui.editingCamera && state.ui.selectedKeyframe != null) {
            state.commandsStack.execute(new SetKeyframeValueCommand(
                    state,
                    state.ui.selectedKeyframe.propertyId(),
                    state.ui.selectedKeyframe.tick(),
                    new Position(
                            state.position.x,
                            state.position.y,
                            state.position.z,
                            state.position.headPitch,
                            state.position.headYaw
                    )
            ));
        }

        state.ui.editingCamera = false;
    }

    protected void handlePage(@Nonnull ReplayState state, @Nonnull PlayerRef playerRef) {
        if (!state.useEditor || state.ui.controlGame) {
            return;
        }

        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null) {
            return;
        }

        Store<EntityStore> store = ref.getStore();
        Player player = store.getComponent(ref, Player.getComponentType());
        assert player != null;

        replacePageManager(playerRef, player);

        EditorUI editorUI;

        CustomUIPage customPage = player.getPageManager().getCustomPage();
        if (customPage instanceof EditorUI ui) {
            editorUI = ui;
        } else if (customPage == null) {
            editorUI = new EditorUI(playerRef, this, state);
            player.getPageManager().openCustomPage(ref, store, editorUI);
        } else {
            return;
        }

        editorUI.tick();
    }

    private void replacePageManager(@Nonnull PlayerRef playerRef, @Nonnull Player player) {
        if (player.getPageManager() instanceof RealtimePageManager) {
            return;
        }

        try {
            Field pageManagerField = Player.class.getDeclaredField("pageManager");
            pageManagerField.setAccessible(true);

            pageManagerField.set(player, new RealtimePageManager(playerRef, player.getWindowManager()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void handleCameraPathDisplay(@Nonnull ReplayState state, @Nonnull PlayerRef playerRef) {
        if (state.stage.isPlaying) {
            state.overlay.clearImmediately(playerRef);
            return;
        }

        if (!state.overlay.shouldRender()) {
            return;
        }

        CameraProperty cameraProperty = (CameraProperty) state.timeline.getProperties().get("camera");

        TreeMap<Integer, Position> values = cameraProperty.getValues();
        if (values.isEmpty()) {
            return;
        }

        List<Position> positions = values.values().stream().toList();

        List<Vector3d> lines = new ArrayList<>();
        int ticksResolution = 10;
        int maxTicksWindow = 30 * 60;

        int from = Math.max(values.firstKey(), state.currentTick - maxTicksWindow);
        int to = Math.min(values.lastKey(), state.currentTick + maxTicksWindow);

        for (int i = from; i <= to; i += ticksResolution) {
            Position position = cameraProperty.getValue(i);
            if (position == null) {
                continue;
            }

            lines.add(new Vector3d(position.x(), position.y(), position.z()));
        }

        Vector3d playerPosition = new Vector3d(state.position.x, state.position.y, state.position.z);

        state.overlay.renderTo(
                playerRef, positions, lines, playerPosition, cameraProperty.getValue((int) state.targetTick)
        );
    }

    public abstract int getDurationTicks(@Nonnull ReplayState state);

    public abstract UUID getSaveUUID(@Nonnull ReplayState state);

}
