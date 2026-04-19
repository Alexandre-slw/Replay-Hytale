package gg.alexandre.replay.ui.editor.renderers;

import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import gg.alexandre.replay.replay.editor.properties.base.BaseProperty;
import gg.alexandre.replay.replay.state.ReplayState;
import gg.alexandre.replay.ui.editor.EditorUI;
import gg.alexandre.replay.ui.event.UIEventContext;
import gg.alexandre.replay.ui.event.UIEventHandler;

import javax.annotation.Nonnull;

public class PropertiesHeaderRenderer extends BaseRenderer<EditorUI.Data> {

    private int propertiesCount = -1;

    public PropertiesHeaderRenderer(ReplayState state) {
        super(state);
    }

    @Override
    public void render(@Nonnull UICommandBuilder uiCommandBuilder, @Nonnull UIEventHandler<EditorUI.Data> eventHandler,
                       @Nonnull ReplayState state, int width) {
        if (propertiesCount == state.timeline.getProperties().size()) {
            return;
        }
        propertiesCount = state.timeline.getProperties().size();

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
                  );
                };
                
                @KeyframeButton = Button {
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

            headers.append(String.format("""
                    @Container {
                      Anchor: (Horizontal: 0, Height: 30, Top: 5);
                      LayoutMode: Left;
                    
                      @RemoveButton #Remove%d {
                      }
                    
                      @Header {
                        @Name = %s;
                        @KeyframeButton #AddKeyframe%d {
                        }
                      }
                    }
                    """, i, "%replay." + property.id(), i));

            int index = i;
            eventHandler.handle(CustomUIEventBindingType.Activating,
                    "#Remove" + index,
                    (_) -> onRemoveProperty(index),
                    true
            );

            eventHandler.handle(CustomUIEventBindingType.Activating,
                    "#AddKeyframe" + index,
                    (data) -> data.append("@Playhead", "#Playhead.Value"),
                    (context) -> onAddKeyframe(context, index),
                    true
            );
        }

        uiCommandBuilder.clear("#PropertiesHeader");
        uiCommandBuilder.appendInline("#PropertiesHeader", headers.toString());
    }

    private void onRemoveProperty(int propertyIndex) {
        // TODO: undo/redo
        state.timeline.getProperties().remove(propertyIndex);
        state.ui.dirtyTimeline = true;
    }

    private void onAddKeyframe(@Nonnull UIEventContext<EditorUI.Data> context, int propertyIndex) {
        BaseProperty property = state.timeline.getProperties().get(propertyIndex);

        // TODO: value
        // TODO: undo/redo
        property.getValues().put(context.data.playhead, property.getDefaultValue(state));
        state.ui.dirtyTimeline = true;
    }

}