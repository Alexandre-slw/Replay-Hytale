package gg.alexandre.replay.ui.editor.renderers;

import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import gg.alexandre.replay.replay.state.ReplayState;
import gg.alexandre.replay.ui.event.UIEventHandler;
import gg.alexandre.replay.ui.event.UIEventIdData;

import javax.annotation.Nonnull;

public abstract class BaseRenderer<T extends UIEventIdData> {

    protected final ReplayState state;

    public BaseRenderer(ReplayState state) {
        this.state = state;
    }

    public abstract void render(@Nonnull UICommandBuilder uiCommandBuilder, @Nonnull UIEventHandler<T> eventHandler,
                                @Nonnull ReplayState state, int width);

    @Nonnull
    protected Anchor anchor(int left, int top, int width, int height) {
        Anchor anchor = new Anchor();
        anchor.setLeft(Value.of(left));
        anchor.setTop(Value.of(top));
        anchor.setWidth(Value.of(width));
        anchor.setHeight(Value.of(height));
        return anchor;
    }
}
