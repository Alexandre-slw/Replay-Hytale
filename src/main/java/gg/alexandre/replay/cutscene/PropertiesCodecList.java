package gg.alexandre.replay.cutscene;

import gg.alexandre.replay.cutscene.codec.CameraCodec;
import gg.alexandre.replay.cutscene.codec.PropertyCodec;
import gg.alexandre.replay.cutscene.codec.RollCodec;
import gg.alexandre.replay.cutscene.codec.SpeedCodec;

import javax.annotation.Nonnull;

public enum PropertiesCodecList {
    CAMERA(1, new CameraCodec()),
    ROLL(2, new RollCodec()),
    SPEED(3, new SpeedCodec());

    private final int id;
    private final PropertyCodec<?> codec;

    PropertiesCodecList(int id, @Nonnull PropertyCodec<?> codec) {
        this.id = id;
        this.codec = codec;
    }

    public int getId() {
        return id;
    }

    @Nonnull
    public PropertyCodec<?> getCodec() {
        return codec;
    }

    static PropertiesCodecList byId(int id) {
        for (PropertiesCodecList codec : values()) {
            if (codec.id == id) {
                return codec;
            }
        }

        throw new IllegalArgumentException("Unknown property codec id: " + id);
    }
}
