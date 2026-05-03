package gg.alexandre.replay.replay.editor.properties;

import com.hypixel.hytale.server.core.entity.entities.Player;
import gg.alexandre.replay.replay.ReplayPlayer;
import gg.alexandre.replay.replay.editor.properties.base.DoubleProperty;
import gg.alexandre.replay.replay.state.ReplayState;
import gg.alexandre.replay.ui.EditKeyframeUI;
import gg.alexandre.replay.ui.event.UIEventContext;

import javax.annotation.Nonnull;

public class SpeedProperty extends DoubleProperty {

    public SpeedProperty() {
        super(1.0);
    }

    @Override
    public void handle(@Nonnull ReplayState state, int tick) {
        Double value = getValue(tick);
        if (value == null) {
            return;
        }

        state.edit.speed = value;
    }

    @Override
    public void editKeyframe(@Nonnull ReplayPlayer player, @Nonnull ReplayState state,
                             @Nonnull Player playerComponent, @Nonnull UIEventContext<?> context, int tick) {
        Double value = getValue(tick);
        if (value == null) {
            return;
        }

        context.store.getExternalData().getWorld().execute(() -> {
            playerComponent.getPageManager().openCustomPage(context.ref, context.store, new EditKeyframeUI(
                    context.playerRef, id(), 1, 1000, (int) (value * 100),
                    (newValue) -> getValues().put(tick, newValue / 100.0),
                    () -> getValues().remove(tick)
            ));
        });
    }

    @Nonnull
    @Override
    public String id() {
        return "speed";
    }
}
