package gg.alexandre.replay.util;

import com.hypixel.hytale.protocol.ModelTransform;
import com.hypixel.hytale.protocol.TransformUpdate;
import com.hypixel.hytale.protocol.packets.player.ClientMovement;
import com.hypixel.hytale.protocol.packets.player.ClientTeleport;
import gg.alexandre.replay.replay.state.ReplayState;

import javax.annotation.Nonnull;

public class PositionTracker {

    public static final double REL_SCALE = 1.0 / 10000.0;
    public static final double TELEPORT_ACK_MAX_DIST_SQ = 0.001;

    public static void onClientMovement(@Nonnull ReplayState state, @Nonnull ClientMovement packet) {
        if (state.cameraManager.isFollowingPath()) {
            return;
        }

        if (state.position.hasPendingTeleport) {
            if (packet.teleportAck != null && packet.absolutePosition != null) {
                int ackId = packet.teleportAck.teleportId & 0xFF;
                if (ackId == state.position.nextExpectedTeleportId) {
                    double dx = packet.absolutePosition.x - state.position.pendingTeleportX;
                    double dy = packet.absolutePosition.y - state.position.pendingTeleportY;
                    double dz = packet.absolutePosition.z - state.position.pendingTeleportZ;
                    double distSq = dx * dx + dy * dy + dz * dz;

                    if (distSq <= TELEPORT_ACK_MAX_DIST_SQ) {
                        state.position.hasPendingTeleport = false;
                        state.position.nextExpectedTeleportId = (state.position.nextExpectedTeleportId + 1) & 0xFF;
                    }
                }
            }
            return;
        }

        if (packet.mountedTo != state.position.mountId) {
            return;
        }

        if (packet.bodyOrientation != null) {
            state.position.bodyPitch = packet.bodyOrientation.pitch;
            state.position.bodyYaw = packet.bodyOrientation.yaw;
            state.position.bodyRoll = packet.bodyOrientation.roll;
        }

        if (packet.lookOrientation != null) {
            state.position.headPitch = packet.lookOrientation.pitch;
            state.position.headYaw = packet.lookOrientation.yaw;
            state.position.headRoll = packet.lookOrientation.roll;
        }

        if (packet.absolutePosition != null) {
            state.position.x = packet.absolutePosition.x;
            state.position.y = packet.absolutePosition.y;
            state.position.z = packet.absolutePosition.z;
        } else if (packet.relativePosition != null) {
            state.position.x += packet.relativePosition.x * REL_SCALE;
            state.position.y += packet.relativePosition.y * REL_SCALE;
            state.position.z += packet.relativePosition.z * REL_SCALE;
        }
    }

    public static void onClientTeleport(@Nonnull ReplayState state, @Nonnull ClientTeleport packet) {
        if (state.cameraManager.isFollowingPath()) {
            return;
        }

        ModelTransform modelTransform = packet.modelTransform;
        if (modelTransform != null && modelTransform.position != null) {
            state.position.x = modelTransform.position.x;
            state.position.y = modelTransform.position.y;
            state.position.z = modelTransform.position.z;
        }

        if (modelTransform != null && modelTransform.bodyOrientation != null) {
            state.position.bodyPitch = modelTransform.bodyOrientation.pitch;
            state.position.bodyYaw = modelTransform.bodyOrientation.yaw;
            state.position.bodyRoll = modelTransform.bodyOrientation.roll;
        }

        if (modelTransform != null && modelTransform.lookOrientation != null) {
            state.position.headPitch = modelTransform.lookOrientation.pitch;
            state.position.headYaw = modelTransform.lookOrientation.yaw;
            state.position.headRoll = modelTransform.lookOrientation.roll;
        }

        state.position.hasPendingTeleport = true;
        state.position.nextExpectedTeleportId = packet.teleportId & 0xFF;
        state.position.pendingTeleportX = state.position.x;
        state.position.pendingTeleportY = state.position.y;
        state.position.pendingTeleportZ = state.position.z;
    }

    public static void onTransformUpdate(@Nonnull ReplayState state, @Nonnull TransformUpdate transformUpdate) {
        if (state.cameraManager.isFollowingPath()) {
            return;
        }

        ModelTransform modelTransform = transformUpdate.transform;

        if (modelTransform.position != null) {
            state.position.x = modelTransform.position.x;
            state.position.y = modelTransform.position.y;
            state.position.z = modelTransform.position.z;
        }

        if (modelTransform.bodyOrientation != null) {
            state.position.bodyPitch = modelTransform.bodyOrientation.pitch;
            state.position.bodyYaw = modelTransform.bodyOrientation.yaw;
            state.position.bodyRoll = modelTransform.bodyOrientation.roll;
        }

        if (modelTransform.lookOrientation != null) {
            state.position.headPitch = modelTransform.lookOrientation.pitch;
            state.position.headYaw = modelTransform.lookOrientation.yaw;
            state.position.headRoll = modelTransform.lookOrientation.roll;
        }
    }
}
