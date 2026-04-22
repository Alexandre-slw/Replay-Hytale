package gg.alexandre.replay.replay.editor.commands;

import gg.alexandre.replay.replay.editor.properties.base.BaseProperty;
import gg.alexandre.replay.replay.state.ReplayState;

import javax.annotation.Nonnull;

public class AddKeyframeCommand extends CommandBase {

    private final String propertyId;
    private final int tick;
    private final Object value;

    public AddKeyframeCommand(@Nonnull ReplayState state, @Nonnull String propertyId, int tick, @Nonnull Object value) {
        super("addKeyframe", state);

        this.propertyId = propertyId;
        this.tick = tick;
        this.value = value;
    }

    @Override
    public void execute() {
        BaseProperty property = state.timeline.getProperties().get(propertyId);
        property.getValues().put(tick, value);
    }

    @Override
    public void undo() {
        BaseProperty property = state.timeline.getProperties().get(propertyId);
        property.getValues().remove(tick);
    }

}
