package gg.alexandre.replay.replay.editor.commands;

import gg.alexandre.replay.replay.editor.properties.base.BaseProperty;
import gg.alexandre.replay.replay.editor.registry.PropertyRegistry;
import gg.alexandre.replay.replay.state.ReplayState;

import javax.annotation.Nonnull;

public class AddPropertyCommand extends CommandBase {

    private final String propertyId;

    public AddPropertyCommand(@Nonnull ReplayState state, @Nonnull String propertyId) {
        super("addProperty", state);

        this.propertyId = propertyId;
    }

    @Override
    public void execute() {
        BaseProperty<?> property = PropertyRegistry.get().create(propertyId);
        if (property == null) {
            return;
        }

        state.timeline.getProperties().put(property.id(), property);
    }

    @Override
    public void undo() {
        state.timeline.getProperties().remove(propertyId);
    }

}
