package gg.alexandre.replay.cutscene.codec;

import gg.alexandre.replay.replay.editor.properties.SpeedProperty;
import gg.alexandre.replay.replay.editor.properties.base.DoubleProperty;

import javax.annotation.Nonnull;

public class SpeedCodec extends DoubleCodec {

    @Nonnull
    @Override
    public DoubleProperty get() {
        return new SpeedProperty();
    }
}
