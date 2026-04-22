package gg.alexandre.replay.replay.editor.commands;

import gg.alexandre.replay.replay.editor.properties.base.BaseProperty;
import gg.alexandre.replay.replay.state.ReplayState;

import javax.annotation.Nonnull;

public class MoveKeyframeCommand extends CommandBase {

    private final String propertyId;
    private final int fromTick;
    private int toTick;

    public MoveKeyframeCommand(@Nonnull ReplayState state, @Nonnull String propertyId, int fromTick,
                               int toTick) {
        super("moveKeyframe", state);

        this.propertyId = propertyId;
        this.fromTick = fromTick;
        this.toTick = toTick;
    }

    @Override
    public void execute() {
        move(fromTick, toTick);
    }

    @Override
    public void undo() {
        move(toTick, fromTick);
    }

    private void move(int from, int to) {
        BaseProperty property = state.timeline.getProperties().get(propertyId);

        Object value = property.getValues().remove(from);
        property.getValues().put(to, value);
    }

    @Override
    public boolean merge(@Nonnull CommandBase other) {
        if (!(other instanceof MoveKeyframeCommand otherMove)) {
            return false;
        }

        if (!propertyId.equals(otherMove.propertyId) || toTick != otherMove.fromTick) {
            return false;
        }

        toTick = otherMove.toTick;
        return true;
    }

}
