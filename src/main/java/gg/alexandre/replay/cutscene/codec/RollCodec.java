package gg.alexandre.replay.cutscene.codec;

import gg.alexandre.replay.replay.editor.properties.RollProperty;
import gg.alexandre.replay.replay.editor.properties.base.DoubleProperty;

import javax.annotation.Nonnull;

public class RollCodec extends DoubleCodec {

    @Nonnull
    @Override
    public DoubleProperty get() {
        return new RollProperty();
    }
}
