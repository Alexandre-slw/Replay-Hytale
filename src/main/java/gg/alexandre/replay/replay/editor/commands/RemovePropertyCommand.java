package gg.alexandre.replay.replay.editor.commands;

import gg.alexandre.replay.replay.editor.properties.base.BaseProperty;
import gg.alexandre.replay.replay.state.ReplayState;

import javax.annotation.Nonnull;

public class RemovePropertyCommand extends CommandBase {

    private final String propertyId;

    private BaseProperty<?> property;

    public RemovePropertyCommand(@Nonnull ReplayState state, @Nonnull String propertyId) {
        super("removeProperty", state);

        this.propertyId = propertyId;
    }

    @Override
    public void execute() {
        property = state.timeline.getProperties().remove(propertyId);
    }

    @Override
    public void undo() {
        if (property != null) {
            state.timeline.getProperties().put(propertyId, property);
        }
    }

}
