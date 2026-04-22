package gg.alexandre.replay.replay.editor.commands;

import gg.alexandre.replay.replay.editor.properties.base.BaseProperty;
import gg.alexandre.replay.replay.state.TimelineState;

import javax.annotation.Nonnull;

public class MoveKeyframeCommand extends CommandBase {

    private TimelineState timelineState;
    private String propertyId;
    private final int fromTick;
    private int toTick;

    public MoveKeyframeCommand(@Nonnull TimelineState timelineState, @Nonnull String propertyId, int fromTick,
                               int toTick) {
        super("moveKeyframe");

        this.timelineState = timelineState;
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
        BaseProperty property = timelineState.getProperties().get(propertyId);

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
