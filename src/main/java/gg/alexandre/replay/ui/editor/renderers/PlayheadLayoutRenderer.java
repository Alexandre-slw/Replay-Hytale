package gg.alexandre.replay.ui.editor.renderers;

import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import gg.alexandre.replay.replay.state.ReplayState;
import gg.alexandre.replay.ui.editor.EditorUI;
import gg.alexandre.replay.ui.event.UIEventHandler;

import javax.annotation.Nonnull;

public class PlayheadLayoutRenderer extends BaseRenderer<EditorUI.Data> {

    public PlayheadLayoutRenderer(@Nonnull ReplayState state) {
        super(state);
    }

    @Override
    public void render(@Nonnull UICommandBuilder uiCommandBuilder, @Nonnull UIEventHandler<EditorUI.Data> eventHandler,
                       @Nonnull ReplayState state, int width) {
        uiCommandBuilder.setObject("#Playhead.Anchor", anchor(0, -20, width + 7, 20));
    }

}
