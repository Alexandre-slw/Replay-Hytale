package gg.alexandre.replay.replay.editor.properties;

import gg.alexandre.replay.replay.editor.properties.base.DoubleProperty;
import gg.alexandre.replay.replay.state.ReplayState;

import javax.annotation.Nonnull;

public class SpeedProperty extends DoubleProperty {

    public SpeedProperty() {
        super(1.0);
    }

    @Override
    public void handle(@Nonnull ReplayState state, int tick) {
        // TODO: implement
    }

    @Nonnull
    @Override
    public String id() {
        return "speed";
    }
}
