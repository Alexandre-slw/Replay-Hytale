package gg.alexandre.replay.ui.editor.renderers;

import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import gg.alexandre.replay.replay.ReplayPlayer;
import gg.alexandre.replay.replay.editor.properties.base.BaseProperty;
import gg.alexandre.replay.replay.state.ReplayState;
import gg.alexandre.replay.replay.state.UIState;
import gg.alexandre.replay.ui.editor.EditorUI;
import gg.alexandre.replay.ui.event.UIEventContext;
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
                    Sounds: (
                      MouseHover: (SoundPath: "Sounds/ButtonsLightHover.ogg", Volume: 6)
                    )
                  );
                };
                """);

        int ticks = state.file.getMetadata().ticks;
        for (BaseProperty<?> property : state.timeline.getProperties().values()) {
            String id = property.id();

            headers.append("@Track {");

            boolean hasSelectedKeyframe = state.ui.selectedKeyframe != null &&
                                          state.ui.selectedKeyframe.propertyId().equals(id);

            if (hasSelectedKeyframe) {
                headers.append(String.format("""
                        Slider #SelectedKeyframe {
                          Anchor: (Height: 30);
                          Value: %d;
                          Min: 0;
                          Max: %d;
                          Style: (
                            Handle: "Assets/SelectedKeyframe.png",
                            HandleWidth: 16,
                            HandleHeight: 16
                          );
                        }
                        """, state.ui.selectedKeyframe.tick(), ticks));

                eventHandler.handle(CustomUIEventBindingType.ValueChanged,
                        "#SelectedKeyframe",
                        (data) -> data.append("@Tick", "#SelectedKeyframe.Value"),
                        this::onKeyframeMove,
                        true
                );

                eventHandler.handle(CustomUIEventBindingType.MouseButtonReleased,
                        "#SelectedKeyframe",
                        (data) -> data.append("@Tick", "#SelectedKeyframe.Value"),
                        this::onKeyframeRelease,
                        true
                );
            }

            for (int tick : property.getValues().keySet()) {
                if (hasSelectedKeyframe && state.ui.selectedKeyframe.tick() == tick) {
                    continue;
                }

                int x = (int) ((tick / (double) ticks) * width) - 8;

                headers.append(String.format("""
                        @Keyframe #KeyframeAt%sTick%d {
                          @Anchor = (Left: %d);
                        }
                        """, id, tick, x));

                eventHandler.handle(CustomUIEventBindingType.Activating,
                        "#KeyframeAt" + id + "Tick" + tick,
                        (data) -> data.append("@Playhead", "#Playhead.Value"),
                        (_) -> onClickKeyframe(id, tick),
                        true
                );

                eventHandler.handle(CustomUIEventBindingType.RightClicking,
                        "#KeyframeAt" + id + "Tick" + tick,
                        (data) -> data.append("@Playhead", "#Playhead.Value"),
                        (context) -> onRightClickKeyframe(context, id, tick),
                        true
                );
            }

            headers.append("}");
        }

        uiCommandBuilder.clear("#Keyframes");
        uiCommandBuilder.appendInline("#Keyframes", headers.toString());
    }

    private void select(@Nonnull String propertyId, int tick, boolean select) {
        if (tick < state.targetTick) {
            player.restart(state);
        }

        state.targetTick = tick;
        state.stage.isPlaying = false;

        if (select) {
            state.ui.selectedKeyframe = new UIState.Keyframe(propertyId, tick);
            state.ui.dirtyTimeline = true;
        } else if (state.ui.selectedKeyframe != null) {
            state.ui.selectedKeyframe = null;
            state.ui.dirtyTimeline = true;
        }
    }

    private void onClickKeyframe(@Nonnull String propertyId, int tick) {
        select(propertyId, tick, true);
    }

    private void onRightClickKeyframe(@Nonnull UIEventContext<EditorUI.Data> context, @Nonnull String propertyId,
                                      int tick) {
        select(propertyId, tick, false);

        BaseProperty<?> property = state.timeline.getProperties().get(propertyId);
        context.store.getExternalData().getWorld().execute(() -> {
            Player playerComponent = context.store.getComponent(context.ref, Player.getComponentType());
            assert playerComponent != null;
            property.editKeyframe(state, playerComponent, context, tick);
        });
    }

    private void moveKeyframe(@Nonnull UIEventContext<EditorUI.Data> context, boolean overwrite) {
        if (state.ui.selectedKeyframe == null) {
            return;
        }

        int tick = context.data.tick;

        BaseProperty property = state.timeline.getProperties().get(state.ui.selectedKeyframe.propertyId());

        if (!overwrite && property.getValues().containsKey(tick)) {
            return;
        }

        // TODO: undo/redo
        Object value = property.getValues().remove(state.ui.selectedKeyframe.tick());
        property.getValues().put(tick, value);

        state.ui.selectedKeyframe = new UIState.Keyframe(state.ui.selectedKeyframe.propertyId(), tick);
    }

    private void onKeyframeMove(@Nonnull UIEventContext<EditorUI.Data> context) {
        moveKeyframe(context, false);
    }

    private void onKeyframeRelease(@Nonnull UIEventContext<EditorUI.Data> context) {
        moveKeyframe(context, true);
        state.ui.selectedKeyframe = null;
        state.ui.dirtyTimeline = true;
    }

}