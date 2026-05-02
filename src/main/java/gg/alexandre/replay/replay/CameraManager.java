package gg.alexandre.replay.replay;

import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.protocol.*;
import com.hypixel.hytale.protocol.packets.camera.SetServerCamera;
import com.hypixel.hytale.protocol.packets.player.ClientTeleport;
import com.hypixel.hytale.protocol.packets.player.SetMovementStates;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.util.PositionUtil;
import gg.alexandre.replay.replay.state.ReplayState;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import java.util.concurrent.atomic.AtomicInteger;

public class CameraManager {

    private static final AtomicInteger NEXT_TELEPORT_ID = new AtomicInteger();

    private boolean followingPath;
    private boolean hasFov;
    private int offset;

    private Rotation3f lastRotation = new Rotation3f();

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
            Rotation3f rotation = new Rotation3f(
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
                lastRotation = rotation;
            }

            if (followingPath) {
                boolean useSmootherRotation = offset != 0;
                teleportPlayer(
                        packetHandler,
                        new Vector3d(0, -offset, 0).add(position),
                        useSmootherRotation ? lastRotation : rotation
                );

                state.position.x = position.x;
                state.position.y = position.y;
                state.position.z = position.z;

                state.position.bodyPitch = 0;
                state.position.bodyRoll = rotation.yaw();
                state.position.bodyYaw = 0;

                state.position.headPitch = rotation.pitch();
                state.position.headYaw = rotation.yaw();
                state.position.headRoll = rotation.roll();

                if (useSmootherRotation) {
                    ServerCameraSettings settings = new ServerCameraSettings();

                    settings.isFirstPerson = false;
                    settings.positionOffset = PositionUtil.toPositionPacket(new Vector3d(0, offset + 1.6, 0));
                    settings.rotation = PositionUtil.toDirectionPacket(rotation);
                    settings.rotationType = RotationType.Custom;
                    settings.sendMouseMotion = false;
                    settings.rotationLerpSpeed = 0.8f;
                    settings.positionLerpSpeed = 0.8f;
                    settings.skipCharacterPhysics = true;
                    settings.allowPitchControls = false;

                    packetHandler.writeNoCache(new SetServerCamera(
                            ClientCameraView.Custom, true, settings
                    ));
                }
            }
        });
    }

    private void teleportPlayer(@Nonnull PacketHandler handler, @Nonnull Vector3d position,
                                @Nonnull Rotation3f rotation) {
        Rotation3f bodyRotation = new Rotation3f(0.0F, rotation.yaw(), 0.0F);

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
