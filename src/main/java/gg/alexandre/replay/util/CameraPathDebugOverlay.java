package gg.alexandre.replay.util;

import com.hypixel.hytale.math.matrix.Matrix4d;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.DebugFlags;
import com.hypixel.hytale.protocol.DebugShape;
import com.hypixel.hytale.protocol.Vector3f;
import com.hypixel.hytale.protocol.packets.player.ClearDebugShapes;
import com.hypixel.hytale.protocol.packets.player.DisplayDebug;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.List;

public class CameraPathDebugOverlay {
    private static final byte FLAG_FADE = (byte) (1 << DebugFlags.Fade.getValue());

    private final Vector3f pointColor;
    private final Vector3f lineColor;
    private final float opacity;
    private final float lifetimeSeconds;
    private final double pointRadius;
    private final double lineThickness;

    private Instant lastRenderTime = Instant.now();

    public CameraPathDebugOverlay() {
        this(
                new Vector3f(0.93f, 0.9f, 0.13f),
                new Vector3f(0.9f, 0.85f, 0.1f),
                0.3f,
                10f,
                0.5,
                0.01
        );
    }

    public CameraPathDebugOverlay(@Nonnull Vector3f pointColor, @Nonnull Vector3f lineColor, float opacity,
                                  float lifetimeSeconds, double pointRadius, double lineThickness) {
        this.pointColor = new Vector3f(pointColor);
        this.lineColor = new Vector3f(lineColor);
        this.opacity = opacity;
        this.lifetimeSeconds = lifetimeSeconds;
        this.pointRadius = pointRadius;
        this.lineThickness = lineThickness;
    }

    private static float getDistanceOpacity(@Nonnull Vector3d playerPosition, @Nonnull Vector3d point) {
        double fadeStart = 1.5;
        double fadeEnd = 3.0;

        double dx = point.x - playerPosition.x;
        double dy = point.y - playerPosition.y;
        double dz = point.z - playerPosition.z;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (distance <= fadeStart) {
            return 0.001f;
        }
        if (distance >= fadeEnd) {
            return 1.0f;
        }

        double t = (distance - fadeStart) / (fadeEnd - fadeStart);
        t = t * t * (3.0 - 2.0 * t);
        return (float) t;
    }

    public boolean shouldRender() {
        return lastRenderTime.plusMillis(500).isBefore(Instant.now());
    }

    public void renderTo(@Nonnull PlayerRef playerRef, List<Position> positions, List<Vector3d> lines,
                         @Nonnull Vector3d playerPosition, @Nullable Position cameraPosition) {
        lastRenderTime = Instant.now();

        clearImmediately(playerRef);
        playerRef.getPacketHandler().tryFlush();

        if (positions.isEmpty()) {
            return;
        }

        for (Position position : positions) {
            playerRef.getPacketHandler().writeNoCache(makeCone(position, pointColor, pointRadius, playerPosition));
        }

        for (int i = 0; i + 1 < lines.size(); i++) {
            Vector3d start = lines.get(i);
            Vector3d end = lines.get(i + 1);
            playerRef.getPacketHandler().writeNoCache(makeLine(start, end, lineColor, lineThickness, playerPosition));
        }

        if (cameraPosition != null) {
            playerRef.getPacketHandler().writeNoCache(makeCone(
                    cameraPosition, new Vector3f(1, 1, 1), pointRadius, playerPosition
            ));
        }

        playerRef.getPacketHandler().tryFlush();
    }

    public void clearImmediately(@Nonnull PlayerRef playerRef) {
        playerRef.getPacketHandler().writeNoCache(new ClearDebugShapes());
    }

    @Nonnull
    private DisplayDebug makeCone(@Nonnull Position position, @Nonnull Vector3f color, double radius,
                                  @Nonnull Vector3d playerPosition) {
        Matrix4d tmp = new Matrix4d();
        Matrix4d matrix = new Matrix4d();
        matrix.identity();

        matrix.translate(position.x(), position.y() + 1.8, position.z());
        matrix.rotateAxis(-position.pitch(), 0.0, 1.0, 0.0, tmp);
        matrix.rotateAxis((Math.PI / 2.0) - position.yaw(), 1.0, 0.0, 0.0, tmp);
        matrix.rotateAxis(Math.PI, 1.0, 0.0, 0.0, tmp);
        matrix.scale(radius, radius, radius);

        float distanceOpacity = getDistanceOpacity(playerPosition, new Vector3d(
                position.x(), position.y(), position.z()
        ));
        return new DisplayDebug(
                DebugShape.Cone,
                matrix.asFloatData(),
                new Vector3f(color),
                lifetimeSeconds,
                FLAG_FADE,
                null,
                opacity * distanceOpacity
        );
    }

    @Nonnull
    private DisplayDebug makeLine(@Nonnull Vector3d start, @Nonnull Vector3d end, @Nonnull Vector3f color,
                                  double thickness, @Nonnull Vector3d playerPosition) {
        double dirX = end.x - start.x;
        double dirY = end.y - start.y;
        double dirZ = end.z - start.z;
        double length = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);

        if (length < 0.001) {
            return null;
        }

        Matrix4d tmp = new Matrix4d();
        Matrix4d matrix = new Matrix4d();
        matrix.identity();
        matrix.translate(start.x, start.y + 1.8, start.z);

        double angleY = Math.atan2(dirZ, dirX);
        matrix.rotateAxis(angleY + (Math.PI / 2.0), 0.0, 1.0, 0.0, tmp);

        double angleX = Math.atan2(Math.sqrt(dirX * dirX + dirZ * dirZ), dirY);
        matrix.rotateAxis(angleX, 1.0, 0.0, 0.0, tmp);

        matrix.translate(0.0, length / 2.0, 0.0);
        matrix.scale(thickness, length, thickness);

        float distanceOpacity = Math.min(
                getDistanceOpacity(playerPosition, start), getDistanceOpacity(playerPosition, end)
        );
        return new DisplayDebug(
                DebugShape.Cube,
                matrix.asFloatData(),
                new Vector3f(color),
                lifetimeSeconds,
                FLAG_FADE,
                null,
                (opacity / 2) * distanceOpacity
        );
    }
}
