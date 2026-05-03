package gg.alexandre.replay.cutscene;

import com.google.gson.Gson;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.SavedMovementStates;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.protocol.packets.player.ClientMovement;
import com.hypixel.hytale.protocol.packets.player.ClientTeleport;
import com.hypixel.hytale.protocol.packets.player.SetMovementStates;
import com.hypixel.hytale.protocol.packets.setup.SetTimeDilation;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PacketFilter;
import com.hypixel.hytale.server.core.io.adapter.PacketWatcher;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import gg.alexandre.replay.ReplayPlugin;
import gg.alexandre.replay.replay.BasePlayer;
import gg.alexandre.replay.replay.editor.properties.base.BaseProperty;
import gg.alexandre.replay.replay.state.ReplayState;
import gg.alexandre.replay.replay.state.TimelineState;
import gg.alexandre.replay.util.Position;
import gg.alexandre.replay.util.PositionTracker;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class CutScenePlayer extends BasePlayer {

    public CutScenePlayer() {
        PacketAdapters.registerInbound((PacketFilter) (handler, packet) -> {
            ReplayState state = getState(handler);
            if (state == null) {
                return false;
            }

            if (packet instanceof SyncInteractionChains syncInteractionChains) {
                handleInteractionChains(handler, state, syncInteractionChains);
                return true;
            }

            if (packet instanceof ClientMovement movement) {
                PositionTracker.onClientMovement(state, movement);
            }

            return false;
        });

        PacketAdapters.registerOutbound((PacketWatcher) (handler, packet) -> {
            ReplayState state = getState(handler);
            if (state == null) {
                return;
            }

            if (packet instanceof ClientTeleport teleport) {
                PositionTracker.onClientTeleport(state, teleport);
            }
        });
    }

    private ReplayState initState(@Nonnull PlayerRef playerRef) {
        ReplayState state = new ReplayState();
        state.playerUuid = playerRef.getUuid();
        state.lang = playerRef.getLanguage();

        Ref<EntityStore> ref = playerRef.getReference();
        assert ref != null;
        Store<EntityStore> store = ref.getStore();

        store.getExternalData().getWorld().execute(() -> {
            TransformComponent transformComponent = store.getComponent(ref, TransformComponent.getComponentType());
            if (transformComponent != null) {
                state.position.x = transformComponent.getPosition().x;
                state.position.y = transformComponent.getPosition().y;
                state.position.z = transformComponent.getPosition().z;
                state.position.headPitch = transformComponent.getRotation().pitch();
                state.position.headYaw = transformComponent.getRotation().yaw();
            }
        });

        states.put(state.playerUuid, state);

        return state;
    }

    public void edit(@Nonnull PlayerRef playerRef, @Nonnull Path path) {
        ReplayState state = initState(playerRef);

        state.path = path;

        try {
            Gson gson = ReplayPlugin.get().getGson();
            state.cutSceneMetadata = gson.fromJson(Files.readString(path), CutSceneMetadata.class);

            state.loadTimelines(getSaveUUID(state));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void play(@Nonnull PlayerRef playerRef, @Nonnull TimelineState timelineState,
                     @Nonnull CutSceneMetadata metadata) {
        ReplayState state = initState(playerRef);

        state.timeline = timelineState;
        state.cutSceneMetadata = metadata;

        state.useEditor = false;
        state.cameraManager.setCutScene(true);

        state.stage.isPlaying = true;
    }

    public void stop(@Nonnull PlayerRef playerRef) {
        ReplayState state = states.get(playerRef.getUuid());
        if (state == null) {
            return;
        }

        state.cameraManager.setDefaultCamera(playerRef.getPacketHandler());

        stop(state);
    }

    public void stop(@Nonnull ReplayState state) {
        states.remove(state.playerUuid);
    }

    public void stopAll() {
        for (ReplayState state : states.values()) {
            stop(state);
        }
    }

    @Override
    public void tick(float v, int i, @Nonnull Store<EntityStore> store) {
        for (ReplayState state : states.values()) {
            PlayerRef playerRef = Universe.get().getPlayer(state.playerUuid);
            if (playerRef == null || !playerRef.isValid()) {
                continue;
            }

            Ref<EntityStore> ref = playerRef.getReference();
            // TODO: use tick on player directly using a component
            if (ref == null || ref.getStore() != store) {
                continue;
            }

            handlePage(state, playerRef);

            tickEditor(state, playerRef);

            if (state.stage.isPlaying) {
                state.targetTick += state.edit.speed;
            }

            if (state.targetTick > getDurationTicks(state)) {
                if (!state.useEditor) {
                    stop(playerRef);
                } else {
                    state.targetTick = getDurationTicks(state);
                }
            }
        }
    }

    private void tickEditor(@Nonnull ReplayState state, @Nonnull PlayerRef playerRef) {
        state.edit.speed = 1.0;
        state.edit.roll = 0.0;

        state.edit.cameraPosition = new Position(
                state.position.x,
                state.position.y,
                state.position.z,
                state.position.headPitch, // yaw and pitch from packets are inverted?
                state.position.headYaw
        );

        if (state.stage.isPlaying && !state.ui.controlGame) {
            for (BaseProperty<?> property : state.timeline.getProperties().values()) {
                property.handle(state, (int) state.targetTick);
            }
        } else {
            playerRef.getPacketHandler().writeNoCache(new SetMovementStates(new SavedMovementStates(true)));
        }

        state.cameraManager.moveCamera(state, playerRef, false);

        handleTimeDilation(state, playerRef.getPacketHandler());

        if (state.timeline.getProperties().containsKey("camera")) {
            handleCameraPathDisplay(state, playerRef);
        }
    }

    private void handleTimeDilation(@Nonnull ReplayState state, @Nonnull PacketHandler packetHandler) {
        float speed = (float) state.edit.speed;

        if (state.timeDilation != speed) {
            state.timeDilation = speed;
            packetHandler.writeNoCache(new SetTimeDilation(Math.min(Math.max(0.0101f, speed), 4)));
        }
    }

    @Override
    public int getDurationTicks(@Nonnull ReplayState state) {
        return state.cutSceneMetadata.ticks;
    }

    @Override
    public UUID getSaveUUID(@NonNullDecl ReplayState state) {
        return state.cutSceneMetadata.uuid;
    }
}
