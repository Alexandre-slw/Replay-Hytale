package gg.alexandre.replay.ui.editor.renderers;

import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import gg.alexandre.replay.replay.ReplayPlayer;
import gg.alexandre.replay.replay.editor.properties.base.BaseProperty;
import gg.alexandre.replay.replay.state.ReplayState;
import gg.alexandre.replay.ui.editor.EditorUI;
import gg.alexandre.replay.ui.event.UIEventHandler;

import javax.annotation.Nonnull;

public class KeyframesRenderer extends BaseRenderer<EditorUI.Data> {

    private final ReplayPlayer player;

    private int lastWidth = -1;

    public KeyframesRenderer(@Nonnull ReplayState state, @Nonnull ReplayPlayer player) {
        super(state);
        this.player = player;
    }

    @Override
    public void render(@Nonnull UICommandBuilder uiCommandBuilder, @Nonnull UIEventHandler<EditorUI.Data> eventHandler,
                       @Nonnull ReplayState state, int width) {
        if (lastWidth == width && !state.ui.dirtyTimeline) {
            return;
        }
        lastWidth = width;
        state.ui.dirtyTimeline = false;

        StringBuilder headers = new StringBuilder();

        headers.append("""
                @Track = Group {
                  Anchor: (Height: 30, Top: 5);
                
                  LayoutMode: Full;
                  Background: #0a0b0c(0.9);
                };
                
                @Keyframe = Button {
                  @Anchor = ();
                  Anchor: (...@Anchor, Width: 16, Height: 16);
                
                  Style: (
                    Default: (
                      Background: (TexturePath: "Assets/Keyframe.png"),
                    ),
                    Hovered: (
                      Background: (TexturePath: "Assets/KeyframeHovered.png"),
                    ),
                    Pressed: (
                      Background: (TexturePath: "Assets/KeyframePressed.png"),
                    ),
                  );
                };
                """);

        for (int i = 0; i < state.timeline.getProperties().size(); i++) {
            BaseProperty<?> property = state.timeline.getProperties().get(i);

            headers.append("@Track {");

            for (int tick : property.getValues().keySet()) {
                int x = (int) ((tick / (double) state.file.getMetadata().ticks) * width) - 8;
                int index = i;

                headers.append(String.format("""
                        @Keyframe #KeyframeAt%dTick%d {
                          @Anchor = (Left: %d);
                        }
                        """, index, tick, x));

                eventHandler.handle(CustomUIEventBindingType.Activating,
                        "#KeyframeAt" + index + "Tick" + tick,
                        (data) -> data.append("@Playhead", "#Playhead.Value"),
                        (_) -> onClickKeyframe(index, tick),
                        true
                );

                eventHandler.handle(CustomUIEventBindingType.RightClicking,
                        "#KeyframeAt" + index + "Tick" + tick,
                        (data) -> data.append("@Playhead", "#Playhead.Value"),
                        (_) -> onRightClickKeyframe(index, tick),
                        true
                );
            }

            headers.append("}");
        }

        uiCommandBuilder.clear("#Keyframes");
        uiCommandBuilder.appendInline("#Keyframes", headers.toString());
    }

    private void gotTo(int tick) {
        if (tick < state.targetTick) {
            player.restart(state);
        }

        state.targetTick = tick;
    }

    private void onClickKeyframe(int propertyIndex, int tick) {
        gotTo(tick);

        BaseProperty<?> property = state.timeline.getProperties().get(propertyIndex);
        Object value = property.getValues().get(tick);

        System.out.println("Clicked keyframe for property " + property.id() + " at tick " + tick + " with value " + value);
    }

    private void onRightClickKeyframe(int propertyIndex, int tick) {
        gotTo(tick);

        BaseProperty<?> property = state.timeline.getProperties().get(propertyIndex);
        Object value = property.getValues().get(tick);

        System.out.println("Right clicked keyframe for property " + property.id() + " at tick " + tick + " with value " + value);
    }

}