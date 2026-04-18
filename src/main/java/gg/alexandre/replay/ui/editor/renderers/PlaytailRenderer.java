package gg.alexandre.replay.ui.editor.renderers;

import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import gg.alexandre.replay.replay.state.ReplayState;

import javax.annotation.Nonnull;

public class PlaytailRenderer extends BaseRenderer {

    private int lastTick = -1;
    private int lastWidth = -1;
    private boolean lastDragging;

    @Override
    public void render(@Nonnull UICommandBuilder uiCommandBuilder, @Nonnull ReplayState state, int width) {
        if (lastTick == state.ui.draggingTick && lastWidth == width && lastDragging == state.ui.dragging) {
            return;
        }
        lastTick = state.ui.draggingTick;
        lastWidth = width;
        lastDragging = state.ui.dragging;

        int x = (int) Math.round(width * (state.ui.draggingTick / (double) state.file.getMetadata().ticks));

        Anchor anchor = new Anchor();
        anchor.setLeft(Value.of(x - width - 30));
        anchor.setTop(Value.of(40));
        anchor.setWidth(Value.of(state.ui.dragging ? 0 : 2));
        uiCommandBuilder.setObject("#Playtail.Anchor", anchor);
    }

}
