package gg.alexandre.replay.cutscene.codec;

import gg.alexandre.replay.replay.editor.properties.base.BaseProperty;
import gg.alexandre.replay.replay.state.TimelineState;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public abstract class PropertyCodec<T extends BaseProperty<?>> {

    @Nonnull
    public abstract T get();

    public abstract void write(@Nonnull T property, @Nonnull TimelineState state, @Nonnull ByteArrayOutputStream out);

    public abstract void read(@Nonnull T property, @Nonnull TimelineState state, @Nonnull ByteBuffer buffer);

}
