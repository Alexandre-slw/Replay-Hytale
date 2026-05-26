package gg.alexandre.replay.replay.editor.properties;

import com.hypixel.hytale.server.core.entity.entities.Player;
import gg.alexandre.replay.replay.BasePlayer;
import gg.alexandre.replay.replay.editor.properties.base.DoubleProperty;
import gg.alexandre.replay.replay.state.ReplayState;
import gg.alexandre.replay.ui.EditKeyframeUI;
import gg.alexandre.replay.ui.event.UIEventContext;

import javax.annotation.Nonnull;

public class FovProperty extends DoubleProperty {

    public FovProperty() {
        super(1.0);
    }

    @Override
    public void handle(@Nonnull ReplayState state, double tick) {
        Double value = getValue(tick);
        if (value == null) {
            return;
        }

        state.edit.fov = value;
    }

    @Override
    public void editKeyframe(@Nonnull BasePlayer player, @Nonnull ReplayState state,
                             @Nonnull Player playerComponent, @Nonnull UIEventContext<?> context, int tick) {
        Double value = getValue(tick);
        if (value == null) {
            return;
        }

        context.store.getExternalData().getWorld().execute(() -> {
            playerComponent.getPageManager().openCustomPage(context.ref, context.store, new EditKeyframeUI(
                    context.playerRef, id(), 10, 200, (int) (value * 100),
                    (newValue) -> getValues().put(tick, newValue / 100.0),
                    () -> getValues().remove(tick)
            ));
        });
    }

    @Nonnull
    @Override
    public String id() {
        return "fov";
    }

    @Override
    public boolean isAvailableInCutScenes() {
        return false;
    }
}
