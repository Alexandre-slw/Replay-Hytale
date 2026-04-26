package gg.alexandre.replay.ui.editor.renderers;

import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import gg.alexandre.replay.replay.state.ReplayState;
import gg.alexandre.replay.ui.editor.EditorUI;
import gg.alexandre.replay.ui.event.UIEventHandler;

import javax.annotation.Nonnull;

public class PlaytailRenderer extends BaseRenderer<EditorUI.Data> {

    private int lastTick = -1;
    private int lastWidth = -1;
    private boolean lastDragging;

    public PlaytailRenderer(@Nonnull ReplayState state) {
        super(state);
    }

    @Override
    public void render(@Nonnull UICommandBuilder uiCommandBuilder, @Nonnull UIEventHandler<EditorUI.Data> eventHandler,
                       @Nonnull ReplayState state, int width) {
        if (lastTick == state.ui.draggingTick && lastWidth == width && lastDragging == state.ui.dragging) {
            return;
        }
        lastTick = state.ui.draggingTick;
        lastWidth = width;
        lastDragging = state.ui.dragging;

        width += 7;

        int x = (int) Math.round(width * (state.ui.draggingTick / (double) state.file.getMetadata().ticks));

        Anchor anchor = new Anchor();
        anchor.setLeft(Value.of(x - 1));
        anchor.setTop(Value.of(0));
        anchor.setWidth(Value.of(state.ui.dragging ? 0 : 2));
        uiCommandBuilder.setObject("#Playtail.Anchor", anchor);
    }

}
