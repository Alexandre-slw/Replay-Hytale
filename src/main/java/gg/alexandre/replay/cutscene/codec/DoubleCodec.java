package gg.alexandre.replay.cutscene.codec;

import gg.alexandre.replay.cutscene.util.ByteBufferUtil;
import gg.alexandre.replay.cutscene.util.PositionEncodeUtil;
import gg.alexandre.replay.replay.editor.properties.base.DoubleProperty;
import gg.alexandre.replay.replay.state.TimelineState;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Map;

public abstract class DoubleCodec extends PropertyCodec<DoubleProperty> {

    @Override
    public void write(@Nonnull DoubleProperty property, @Nonnull TimelineState state,
                      @Nonnull ByteArrayOutputStream out) {
        ByteBufferUtil.writeVarInt(out, property.getValues().size());

        int previousTick = 0;
        int previousRoll = 0;

        for (Map.Entry<Integer, Double> entry : property.getValues().entrySet()) {
            int tick = entry.getKey();
            int rollValue = PositionEncodeUtil.packAngle(entry.getValue());

            ByteBufferUtil.writeVarInt(out, tick - previousTick);
            ByteBufferUtil.writeSignedVarInt(out, rollValue - previousRoll);

            previousTick = tick;
            previousRoll = rollValue;
        }
    }

    @Override
    public void read(@Nonnull DoubleProperty property, @Nonnull TimelineState state, @Nonnull ByteBuffer buffer) {
        int count = ByteBufferUtil.readVarInt(buffer);

        int previousTick = 0;
        int previousRoll = 0;

        for (int i = 0; i < count; i++) {
            int tick = previousTick + ByteBufferUtil.readVarInt(buffer);
            int rollValue = previousRoll + ByteBufferUtil.readSignedVarInt(buffer);

            property.getValues().put(tick, PositionEncodeUtil.unpackAngle(rollValue));

            previousTick = tick;
            previousRoll = rollValue;
        }
    }
}
