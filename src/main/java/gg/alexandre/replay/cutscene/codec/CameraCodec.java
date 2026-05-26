package gg.alexandre.replay.cutscene.codec;

import gg.alexandre.replay.cutscene.util.ByteBufferUtil;
import gg.alexandre.replay.cutscene.util.PositionEncodeUtil;
import gg.alexandre.replay.replay.editor.properties.CameraProperty;
import gg.alexandre.replay.replay.state.TimelineState;
import gg.alexandre.replay.util.Position;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Map;

public class CameraCodec extends PropertyCodec<CameraProperty> {

    @Nonnull
    @Override
    public CameraProperty get() {
        return new CameraProperty();
    }

    @Override
    public void write(@Nonnull CameraProperty property, @Nonnull TimelineState state,
                      @Nonnull ByteArrayOutputStream out) {
        ByteBufferUtil.writeVarInt(out, property.getValues().size());

        int previousTick = 0;

        int previousX = 0;
        int previousY = 0;
        int previousZ = 0;
        int previousYaw = 0;
        int previousPitch = 0;

        for (Map.Entry<Integer, Position> entry : property.getValues().entrySet()) {
            int tick = entry.getKey();
            Position position = entry.getValue();

            int x = PositionEncodeUtil.packPosition(position.x());
            int y = PositionEncodeUtil.packPosition(position.y());
            int z = PositionEncodeUtil.packPosition(position.z());
            int yaw = PositionEncodeUtil.packAngle(position.yaw());
            int pitch = PositionEncodeUtil.packAngle(position.pitch());

            ByteBufferUtil.writeVarInt(out, tick - previousTick);

            ByteBufferUtil.writeSignedVarInt(out, x - previousX);
            ByteBufferUtil.writeSignedVarInt(out, y - previousY);
            ByteBufferUtil.writeSignedVarInt(out, z - previousZ);
            ByteBufferUtil.writeSignedVarInt(out, yaw - previousYaw);
            ByteBufferUtil.writeSignedVarInt(out, pitch - previousPitch);

            previousTick = tick;

            previousX = x;
            previousY = y;
            previousZ = z;
            previousYaw = yaw;
            previousPitch = pitch;
        }
    }

    @Override
    public void read(@Nonnull CameraProperty property, @Nonnull TimelineState state, @Nonnull ByteBuffer buffer) {
        int count = ByteBufferUtil.readVarInt(buffer);

        int previousTick = 0;

        int previousX = 0;
        int previousY = 0;
        int previousZ = 0;
        int previousYaw = 0;
        int previousPitch = 0;

        for (int i = 0; i < count; i++) {
            int tick = previousTick + ByteBufferUtil.readVarInt(buffer);

            int x = previousX + ByteBufferUtil.readSignedVarInt(buffer);
            int y = previousY + ByteBufferUtil.readSignedVarInt(buffer);
            int z = previousZ + ByteBufferUtil.readSignedVarInt(buffer);
            int yaw = previousYaw + ByteBufferUtil.readSignedVarInt(buffer);
            int pitch = previousPitch + ByteBufferUtil.readSignedVarInt(buffer);

            property.getValues().put(
                    tick,
                    new Position(
                            PositionEncodeUtil.unpackPosition(x),
                            PositionEncodeUtil.unpackPosition(y),
                            PositionEncodeUtil.unpackPosition(z),
                            PositionEncodeUtil.unpackAngle(yaw),
                            PositionEncodeUtil.unpackAngle(pitch)
                    )
            );

            previousTick = tick;

            previousX = x;
            previousY = y;
            previousZ = z;
            previousYaw = yaw;
            previousPitch = pitch;
        }
    }
}
