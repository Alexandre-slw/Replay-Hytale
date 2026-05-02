package gg.alexandre.replay.cutscene;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.tick.TickingSystem;
import com.hypixel.hytale.protocol.packets.setup.SetTimeDilation;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import gg.alexandre.replay.replay.editor.properties.base.BaseProperty;
import gg.alexandre.replay.replay.state.ReplayState;
import gg.alexandre.replay.replay.state.TimelineState;
import gg.alexandre.replay.util.Position;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CutScenePlayer extends TickingSystem<EntityStore> {

    private final Map<UUID, ReplayState> states = new ConcurrentHashMap<>();

    public void start(@Nonnull PlayerRef playerRef, @Nonnull TimelineState timelineState) {
        ReplayState state = new ReplayState();
        state.playerUuid = playerRef.getUuid();
        state.lang = playerRef.getLanguage();

        state.timeline = timelineState;

        state.cameraManager.setCutScene(true);

        states.put(state.playerUuid, state);
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

            tickEditor(state, playerRef);

            state.targetTick += state.edit.speed;

            // TODO: stop when ended
            if (state.targetTick > 100) {
                stop(playerRef);
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

        for (BaseProperty<?> property : state.timeline.getProperties().values()) {
            property.handle(state, (int) state.targetTick);
        }

        state.cameraManager.moveCamera(state, playerRef, true);

        handleTimeDilation(state, playerRef.getPacketHandler());
    }

    private void handleTimeDilation(@Nonnull ReplayState state, @Nonnull PacketHandler packetHandler) {
        float speed = (float) state.edit.speed;

        if (state.timeDilation != speed) {
            state.timeDilation = speed;
            packetHandler.writeNoCache(new SetTimeDilation(Math.min(Math.max(0.0101f, speed), 4)));
        }
    }

}
