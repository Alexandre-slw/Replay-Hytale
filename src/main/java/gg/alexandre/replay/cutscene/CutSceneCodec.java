package gg.alexandre.replay.cutscene;

import com.github.luben.zstd.Zstd;
import gg.alexandre.replay.cutscene.codec.PropertyCodec;
import gg.alexandre.replay.cutscene.util.Base85;
import gg.alexandre.replay.cutscene.util.ByteBufferUtil;
import gg.alexandre.replay.replay.editor.properties.base.BaseProperty;
import gg.alexandre.replay.replay.state.TimelineState;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class CutSceneCodec {

    private static final int VERSION = 1;

    private static final int MODE_RAW = 0;
    private static final int MODE_ZSTD = 1;

    private static final int ZSTD_LEVEL = 22;

    @Nonnull
    public static String toData(TimelineState state) {
        byte[] raw = writeRaw(state);
        byte[] zstd = Zstd.compress(raw, ZSTD_LEVEL);

        byte[] packed;

        if (zstd.length + 1 + ByteBufferUtil.varIntSize(raw.length) < raw.length + 1) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(MODE_ZSTD);
            ByteBufferUtil.writeVarInt(out, raw.length);
            out.writeBytes(zstd);
            packed = out.toByteArray();
        } else {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(MODE_RAW);
            out.writeBytes(raw);
            packed = out.toByteArray();
        }

        return Base85.encodeBase85(packed);
    }

    @Nonnull
    public static TimelineState fromData(String data) {
        byte[] packed = Base85.decodeBase85(data);

        ByteBuffer buffer = ByteBuffer.wrap(packed);

        int mode = ByteBufferUtil.readUnsignedByte(buffer);

        byte[] raw;

        if (mode == MODE_RAW) {
            raw = new byte[buffer.remaining()];
            buffer.get(raw);
        } else if (mode == MODE_ZSTD) {
            int rawLength = ByteBufferUtil.readVarInt(buffer);

            byte[] compressed = new byte[buffer.remaining()];
            buffer.get(compressed);

            raw = Zstd.decompress(compressed, rawLength);
        } else {
            throw new IllegalArgumentException("Invalid data mode: " + mode);
        }

        return readRaw(raw);
    }

    private static byte[] writeRaw(TimelineState state) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        out.write(VERSION);

        int propertyCount = 0;

        for (PropertiesCodecList codec : PropertiesCodecList.values()) {
            BaseProperty<?> property = codec.getCodec().get();
            if (state.getProperties().containsKey(property.id())) {
                propertyCount++;
            }
        }

        ByteBufferUtil.writeVarInt(out, propertyCount);

        for (PropertiesCodecList entry : PropertiesCodecList.values()) {
            BaseProperty<?> property = entry.getCodec().get();
            if (state.getProperties().containsKey(property.id())) {
                ByteBufferUtil.writeVarInt(out, entry.getId());
                ((PropertyCodec) entry.getCodec()).write(state.getProperties().get(property.id()), state, out);
            }
        }

        return out.toByteArray();
    }

    private static TimelineState readRaw(byte[] raw) {
        ByteBuffer buffer = ByteBuffer.wrap(raw);

        int version = ByteBufferUtil.readUnsignedByte(buffer);

        if (version != VERSION) {
            throw new IllegalArgumentException("Unsupported cutscene data version: " + version);
        }

        TimelineState state = new TimelineState();

        int propertyCount = ByteBufferUtil.readVarInt(buffer);

        for (int i = 0; i < propertyCount; i++) {
            int propertyId = ByteBufferUtil.readVarInt(buffer);

            PropertiesCodecList entry = PropertiesCodecList.byId(propertyId);
            BaseProperty<?> property = entry.getCodec().get();
            ((PropertyCodec) entry.getCodec()).read(property, state, buffer);

            state.getProperties().put(property.id(), property);
        }

        if (buffer.hasRemaining()) {
            throw new IllegalArgumentException("Trailing unread bytes: " + buffer.remaining());
        }

        return state;
    }

}