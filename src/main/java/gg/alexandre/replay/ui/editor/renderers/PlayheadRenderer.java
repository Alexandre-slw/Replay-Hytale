package gg.alexandre.replay.ui.editor.renderers;

import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import gg.alexandre.replay.replay.state.ReplayState;

import javax.annotation.Nonnull;

public class PlayheadRenderer extends BaseRenderer {

    @Override
    public void layout(@Nonnull UICommandBuilder uiCommandBuilder, @Nonnull ReplayState state, int width) {
        uiCommandBuilder.setObject("#Playhead.Anchor", anchor(0, 0, width, 16));
    }

}
