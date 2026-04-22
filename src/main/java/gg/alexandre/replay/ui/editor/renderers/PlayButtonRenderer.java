package gg.alexandre.replay.ui.editor.renderers;

import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import gg.alexandre.replay.replay.state.ReplayState;
import gg.alexandre.replay.ui.editor.EditorUI;
import gg.alexandre.replay.ui.event.UIEventContext;
import gg.alexandre.replay.ui.event.UIEventHandler;

import javax.annotation.Nonnull;

public class PlayButtonRenderer extends BaseRenderer<EditorUI.Data> {

    private Boolean lastPlay = null;

    public PlayButtonRenderer(@Nonnull ReplayState state) {
        super(state);
    }

    @Override
    public void render(@Nonnull UICommandBuilder uiCommandBuilder, @Nonnull UIEventHandler<EditorUI.Data> eventHandler,
                       @Nonnull ReplayState state, int width) {
        if (lastPlay != null && lastPlay == state.stage.isPlaying) {
            return;
        }
        lastPlay = state.stage.isPlaying;

        String id = lastPlay ? "Play" : "Pause";
        String translation = lastPlay ? "play" : "pause";
        String button = String.format("""
                Button #Play {
                  Anchor: (Width: 20, Height: 20, Left: 10);
                  TooltipText: %%replay.%s;
                  Style: (
                    Default: (
                      Background: (TexturePath: "Assets/%s.png"),
                    ),
                    Hovered: (
                      Background: (TexturePath: "Assets/%sHovered.png"),
                    ),
                    Pressed: (
                      Background: (TexturePath: "Assets/%sPressed.png"),
                    ),
                    Sounds: (
                      MouseHover: (SoundPath: "Sounds/ButtonsLightHover.ogg", Volume: 6)
                    )
                  );
                }
                """, translation, id, id, id);

        uiCommandBuilder.clear("#PlayPauseContainer");
        uiCommandBuilder.appendInline("#PlayPauseContainer", button);

        eventHandler.handle(CustomUIEventBindingType.Activating,
                "#Play",
                this::onPause,
                true
        );
    }

    private void onPause(@Nonnull UIEventContext<EditorUI.Data> context) {
        state.stage.isPlaying = !state.stage.isPlaying;
    }

}