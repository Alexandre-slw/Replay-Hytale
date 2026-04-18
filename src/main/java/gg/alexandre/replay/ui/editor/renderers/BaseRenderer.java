package gg.alexandre.replay.ui.editor.renderers;

import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import gg.alexandre.replay.replay.state.ReplayState;

import javax.annotation.Nonnull;

public abstract class BaseRenderer {

    public abstract void layout(@Nonnull UICommandBuilder uiCommandBuilder, @Nonnull ReplayState state, int width);

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
