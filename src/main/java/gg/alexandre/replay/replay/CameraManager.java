package gg.alexandre.replay.replay;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.protocol.*;
import com.hypixel.hytale.protocol.packets.camera.SetServerCamera;
import com.hypixel.hytale.protocol.packets.player.ClientTeleport;
import com.hypixel.hytale.protocol.packets.player.SetMovementStates;
import com.hypixel.hytale.protocol.packets.player.UpdateMovementSettings;
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
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

    private boolean cutScene = false;
    private boolean appliedFreeCameraMovementSettings;
    private long nextFreeCameraMovementSettingsNanos;

    public void moveCamera(@Nonnull ReplayState state, @Nonnull PlayerRef playerRef, boolean force) {
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
        if (!cutScene && !state.ui.controlGame) {
            applyFreeCameraMovement(playerRef);
        }

        if (stoppedFollowing || fovEnabled) {
            setDefaultCamera(packetHandler);
            teleportPlayer(packetHandler, position, rotation);
        } else if ((startedFollowing || fovDisabled) && followingPath && !hasFov &&
                   (state.cutSceneMetadata == null || cutScene)) {
            offset = 1000;
            lastRotation = rotation;
        }

        if (followingPath) {
            boolean useSmootherRotation = offset != 0;
            if (!cutScene) {
                teleportPlayer(
                        packetHandler,
                        new Vector3d(0, -offset, 0).add(position),
                        useSmootherRotation ? lastRotation : rotation
                );
            }

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

                if (cutScene) {
                    settings.position = PositionUtil.toPositionPacket(position.add(0, 1.6, 0));
                    settings.positionType = PositionType.Custom;
                } else {
                    settings.positionOffset = PositionUtil.toPositionPacket(new Vector3d(0, offset + 1.6, 0));
                }

                settings.rotation = PositionUtil.toDirectionPacket(rotation);
                settings.rotationType = RotationType.Custom;
                settings.rotationLerpSpeed = 0.15f;
                settings.positionLerpSpeed = 0.15f;

                if (!cutScene) {
                    settings.sendMouseMotion = false;
                    settings.skipCharacterPhysics = true;
                    settings.allowPitchControls = false;
                }

                packetHandler.writeNoCache(new SetServerCamera(
                        ClientCameraView.Custom, true, settings
                ));
            }
        }
    }

    public void applyFreeCameraMovement(@Nonnull PlayerRef playerRef) {
        PacketHandler handler = playerRef.getPacketHandler();
        handler.writeNoCache(new SetMovementStates(new SavedMovementStates(true)));

        long now = System.nanoTime();
        if (appliedFreeCameraMovementSettings && now < nextFreeCameraMovementSettingsNanos) {
            return;
        }

        MovementSettings settings = getCurrentMovementSettings(playerRef);
        if (settings == null) {
            return;
        }

        settings.canFly = true;
        settings.horizontalFlySpeed = Math.max(settings.horizontalFlySpeed, 10.32F);
        settings.verticalFlySpeed = Math.max(settings.verticalFlySpeed, 10.32F);

        handler.writeNoCache(new UpdateMovementSettings(settings));
        appliedFreeCameraMovementSettings = true;
        nextFreeCameraMovementSettingsNanos = now + 1_000_000_000L;
    }

    public void restoreMovementSettings(@Nonnull PlayerRef playerRef) {
        MovementSettings settings = getCurrentMovementSettings(playerRef);
        if (settings != null) {
            playerRef.getPacketHandler().writeNoCache(new UpdateMovementSettings(settings));
        }

        appliedFreeCameraMovementSettings = false;
        nextFreeCameraMovementSettingsNanos = 0;
    }

    private MovementSettings getCurrentMovementSettings(@Nonnull PlayerRef playerRef) {
        Ref<EntityStore> ref = playerRef.getReference();
        assert ref != null;
        Store<EntityStore> store = ref.getStore();

        MovementManager movementManager = store.getComponent(ref, MovementManager.getComponentType());
        if (movementManager == null) {
            return null;
        }

        MovementSettings settings = movementManager.getSettings();
        if (settings == null) {
            settings = movementManager.getDefaultSettings();
        }

        return settings != null ? new MovementSettings(settings) : null;
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
        handler.writeNoCache(new SetServerCamera(ClientCameraView.FirstPerson, !cutScene, null));
        offset = 0;
    }

    public void setDefaultCamera(@Nonnull PlayerRef playerRef) {
        setDefaultCamera(playerRef.getPacketHandler());
        restoreMovementSettings(playerRef);
    }

    public boolean isFollowingPath() {
        return followingPath;
    }

    public void setCutScene(boolean cutScene) {
        this.cutScene = cutScene;
    }
}
