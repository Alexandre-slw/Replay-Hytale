package gg.alexandre.replay.replay;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.ClientCameraView;
import com.hypixel.hytale.protocol.ModelTransform;
import com.hypixel.hytale.protocol.SavedMovementStates;
import com.hypixel.hytale.protocol.ServerCameraSettings;
import com.hypixel.hytale.protocol.packets.camera.SetServerCamera;
import com.hypixel.hytale.protocol.packets.player.ClientTeleport;
import com.hypixel.hytale.protocol.packets.player.SetMovementStates;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.util.PositionUtil;
import gg.alexandre.replay.replay.state.ReplayState;

import javax.annotation.Nonnull;
import java.util.concurrent.atomic.AtomicInteger;

public class CameraManager {

    private static final AtomicInteger NEXT_TELEPORT_ID = new AtomicInteger();

    private boolean followingPath;
    private boolean hasFov;
    private int offset;

    public void moveCamera(@Nonnull ReplayState state, @Nonnull PlayerRef playerRef, @Nonnull ReplayPlayer player,
                           boolean force) {
        player.bypassFilter(state, () -> {
            boolean hadFov = hasFov;
            hasFov = state.edit.fov != 1.0;

            boolean wasFollowingPath = followingPath;
            followingPath = (state.stage.isPlaying && !state.ui.controlGame) || force;

            boolean startedFollowing = !wasFollowingPath && followingPath;
            boolean stoppedFollowing = wasFollowingPath && !followingPath;
            boolean fovEnabled = !hadFov && hasFov;
            boolean fovDisabled = hadFov && !hasFov;

            Vector3d position = new Vector3d(
                    state.edit.cameraPosition.x(), state.edit.cameraPosition.y(), state.edit.cameraPosition.z()
            );
            Vector3f rotation = new Vector3f(
                    (float) state.edit.cameraPosition.yaw(),
                    (float) state.edit.cameraPosition.pitch(),
                    (float) Math.toRadians(-state.edit.roll)
            );

            PacketHandler packetHandler = playerRef.getPacketHandler();
            packetHandler.writeNoCache(new SetMovementStates(new SavedMovementStates(true)));

            if (stoppedFollowing || fovEnabled) {
                setDefaultCamera(packetHandler);
                teleportPlayer(packetHandler, position, rotation);
            } else if ((startedFollowing || fovDisabled) && followingPath && !hasFov) {
                offset = 1000;

                ServerCameraSettings settings = new ServerCameraSettings();

                settings.isFirstPerson = false;
                settings.positionOffset = PositionUtil.toPositionPacket(new Vector3d(0, offset + 1.6, 0));
                settings.sendMouseMotion = false;
                settings.rotationLerpSpeed = 0.5f;
                settings.positionLerpSpeed = 0.5f;

                packetHandler.writeNoCache(new SetServerCamera(
                        ClientCameraView.Custom, true, settings
                ));
            }

            if (followingPath) {
                teleportPlayer(packetHandler, new Vector3d(0, -offset, 0).add(position), rotation);

                state.position.x = position.x;
                state.position.y = position.y;
                state.position.z = position.z;

                state.position.bodyPitch = 0;
                state.position.bodyRoll = rotation.getYaw();
                state.position.bodyYaw = 0;

                state.position.headPitch = rotation.getPitch();
                state.position.headYaw = rotation.getYaw();
                state.position.headRoll = rotation.getRoll();
            }
        });
    }

    private void teleportPlayer(@Nonnull PacketHandler handler, @Nonnull Vector3d position,
                                @Nonnull Vector3f rotation) {
        Vector3f bodyRotation = new Vector3f(0.0F, rotation.getYaw(), 0.0F);

        ModelTransform transform = new ModelTransform(
                PositionUtil.toPositionPacket(position),
                PositionUtil.toDirectionPacket(bodyRotation),
                PositionUtil.toDirectionPacket(rotation)
        );

        handler.writeNoCache(new ClientTeleport(
                (byte) NEXT_TELEPORT_ID.getAndIncrement(),
                transform,
                true
        ));
    }

    public void setDefaultCamera(@Nonnull PacketHandler handler) {
        handler.writeNoCache(new SetServerCamera(ClientCameraView.FirstPerson, true, null));
        offset = 0;
    }

    public boolean isFollowingPath() {
        return followingPath;
    }
}
