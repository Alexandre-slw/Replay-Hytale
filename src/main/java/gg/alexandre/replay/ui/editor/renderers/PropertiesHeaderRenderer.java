package gg.alexandre.replay.ui.editor.renderers;

import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import gg.alexandre.replay.replay.editor.properties.base.BaseProperty;
import gg.alexandre.replay.replay.state.ReplayState;
import gg.alexandre.replay.replay.state.UIState;
import gg.alexandre.replay.ui.editor.EditorUI;
import gg.alexandre.replay.ui.event.UIEventContext;
import gg.alexandre.replay.ui.event.UIEventHandler;

import javax.annotation.Nonnull;

public class PropertiesHeaderRenderer extends BaseRenderer<EditorUI.Data> {

    private int propertiesCount = -1;
    private UIState.Keyframe lastSelectedKeyframe = null;

    public PropertiesHeaderRenderer(@Nonnull ReplayState state) {
        super(state);
    }

    @Override
    public void render(@Nonnull UICommandBuilder uiCommandBuilder, @Nonnull UIEventHandler<EditorUI.Data> eventHandler,
                       @Nonnull ReplayState state, int width) {
        if (propertiesCount == state.timeline.getProperties().size() &&
            lastSelectedKeyframe == state.ui.selectedKeyframe) {
            return;
        }
        propertiesCount = state.timeline.getProperties().size();
        lastSelectedKeyframe = state.ui.selectedKeyframe;

        StringBuilder headers = new StringBuilder();

        headers.append("""
                @Container = Group {
                  Anchor: (Horizontal: 0, Height: 30, Top: 5);
                  LayoutMode: Left;
                };
                
                @RemoveButton = Button {
                  Anchor: (Width: 12, Height: 12, Top: 9);
                  TooltipText: %replay.removeProperty;
                  Style: (
                    Default: (
                      Background: (TexturePath: "Assets/Close.png"),
                    ),
                    Hovered: (
                      Background: (TexturePath: "Assets/CloseHovered.png"),
                    ),
                    Pressed: (
                      Background: (TexturePath: "Assets/ClosePressed.png"),
                    ),
                    Sounds: (
                      MouseHover: (SoundPath: "Sounds/ButtonsLightHover.ogg", Volume: 6)
                    )
                  );
                };
                
                @AddKeyframeButton = Button {
                  Anchor: (Width: 20, Height: 20, Right: 6, Top: 5);
                  TooltipText: %replay.addKeyframe;
                  Style: (
                    Default: (
                      Background: (TexturePath: "Assets/ButtonKeyframePlus.png"),
                    ),
                    Hovered: (
                      Background: (TexturePath: "Assets/ButtonKeyframePlusHovered.png"),
                    ),
                    Pressed: (
                      Background: (TexturePath: "Assets/ButtonKeyframePlusPressed.png"),
                    ),
                    Sounds: (
                      MouseHover: (SoundPath: "Sounds/ButtonsLightHover.ogg", Volume: 6)
                    )
                  );
                };
                
                @RemoveKeyframeButton = Button {
                  Anchor: (Width: 20, Height: 20, Right: 6, Top: 5);
                  TooltipText: %replay.removeKeyframe;
                  Style: (
                    Default: (
                      Background: (TexturePath: "Assets/ButtonKeyframeMinus.png"),
                    ),
                    Hovered: (
                      Background: (TexturePath: "Assets/ButtonKeyframeMinusHovered.png"),
                    ),
                    Pressed: (
                      Background: (TexturePath: "Assets/ButtonKeyframeMinusPressed.png"),
                    ),
                    Sounds: (
                      MouseHover: (SoundPath: "Sounds/ButtonsLightHover.ogg", Volume: 6)
                    )
                  );
                };
                
                @Header = Group {
                  @Name = "";
                  FlexWeight: 1;
                  Anchor: (Left: 6);
                  Background: (TexturePath: "Common/InputBinding.png", Border: 6);
                  LayoutMode: Left;
                
                  Label {
                    FlexWeight: 1;
                    Text: @Name;
                    Anchor: (Vertical: 0, Left: 6);
                    Style: (FontSize: 14, TextColor: #ccd8e8, VerticalAlignment: Center);
                  }
                };
                """);

        for (int i = 0; i < state.timeline.getProperties().size(); i++) {
            BaseProperty<?> property = state.timeline.getProperties().get(i);

            boolean hasSelectedKeyframe = state.ui.selectedKeyframe != null &&
                                          state.ui.selectedKeyframe.propertyIndex() == i;

            headers.append(String.format("""
                    @Container {
                      Anchor: (Horizontal: 0, Height: 30, Top: 5);
                      LayoutMode: Left;
                    
                      @RemoveButton #Remove%d {
                      }
                    
                      @Header {
                        @Name = %s;
                        @%sKeyframeButton #KeyframeButton%d {
                        }
                      }
                    }
                    """, i, "%replay." + property.id(), hasSelectedKeyframe ? "Remove" : "Add", i));

            int index = i;
            eventHandler.handle(CustomUIEventBindingType.Activating,
                    "#Remove" + index,
                    (_) -> onRemoveProperty(index),
                    true
            );

            eventHandler.handle(CustomUIEventBindingType.Activating,
                    "#KeyframeButton" + index,
                    (data) -> data.append("@Playhead", "#Playhead.Value"),
                    (context) -> onKeyframeButton(context, index),
                    true
            );
        }

        uiCommandBuilder.clear("#PropertiesHeader");
        uiCommandBuilder.appendInline("#PropertiesHeader", headers.toString());
    }

    private void onRemoveProperty(int propertyIndex) {
        // TODO: undo/redo
        state.timeline.getProperties().remove(propertyIndex);
        state.ui.selectedKeyframe = null;
        state.ui.dirtyTimeline = true;
    }

    private void onKeyframeButton(@Nonnull UIEventContext<EditorUI.Data> context, int propertyIndex) {
        boolean hasSelectedKeyframe = state.ui.selectedKeyframe != null &&
                                      state.ui.selectedKeyframe.propertyIndex() == propertyIndex;

        BaseProperty property = state.timeline.getProperties().get(propertyIndex);

        if (hasSelectedKeyframe) {
            // TODO: undo/redo
            property.getValues().remove(state.ui.selectedKeyframe.tick());
        } else {
            // TODO: value
            // TODO: undo/redo
            property.getValues().put(context.data.playhead, property.getDefaultValue(state));
        }

        state.ui.selectedKeyframe = null;
        state.ui.dirtyTimeline = true;
    }

}