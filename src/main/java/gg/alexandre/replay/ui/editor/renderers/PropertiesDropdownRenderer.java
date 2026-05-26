package gg.alexandre.replay.ui.editor.renderers;

import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import gg.alexandre.replay.replay.editor.commands.AddPropertyCommand;
import gg.alexandre.replay.replay.editor.properties.base.BaseProperty;
import gg.alexandre.replay.replay.editor.registry.PropertyRegistry;
import gg.alexandre.replay.replay.state.ReplayState;
import gg.alexandre.replay.ui.common.CommonUI;
import gg.alexandre.replay.ui.editor.EditorUI;
import gg.alexandre.replay.ui.event.UIEventContext;
import gg.alexandre.replay.ui.event.UIEventHandler;

import javax.annotation.Nonnull;

public class PropertiesDropdownRenderer extends BaseRenderer<EditorUI.Data> {

    private int lastPropertiesCount = -1;

    public PropertiesDropdownRenderer(@Nonnull ReplayState state) {
        super(state);
    }

    @Override
    public void render(@Nonnull UICommandBuilder uiCommandBuilder, @Nonnull UIEventHandler<EditorUI.Data> eventHandler,
                       @Nonnull ReplayState state, int width) {
        int count = state.timeline.getProperties().size();
        if (lastPropertiesCount == count) {
            return;
        }
        lastPropertiesCount = count;

        PropertyRegistry registry = PropertyRegistry.get();

        StringBuilder dropdown = new StringBuilder(CommonUI.DEFAULT_DROPDOWN_STYLE);

        dropdown.append(String.format("""
                Group {
                  DropdownBox #AddProperty {
                    Anchor: (Full: 0);
                    Disabled: %s;
                    Style: (
                      ...@DefaultDropdownBoxStyle,
                      EntriesInViewport: 3,
                      ArrowWidth: 0,
                      LabelStyle: (TextColor: #000000(0))
                    );
                    Value: "";
                """, count >= registry.getRegistry().size()));

        boolean hasProperties = false;
        for (String id : registry.getRegistry().keySet()) {
            if (state.timeline.getProperties().containsKey(id)) {
                continue;
            }

            if (state.cutSceneMetadata != null) {
                BaseProperty<?> baseProperty = registry.create(id);
                if (baseProperty == null || !baseProperty.isAvailableInCutScenes()) {
                    continue;
                }
            }

            dropdown.append(String.format("""
                    DropdownEntry {
                      Value: "%s";
                      Text: %%replay.%s;
                    }
                    """, id, id));

            hasProperties = true;
        }

        dropdown.append(String.format("""
                  }
                }
                
                Label #AddPropertyLabel {
                  Text: %%replay.plusAddProperty;
                  Anchor: (Full: 0);
                  Style: (FontSize: 14, TextColor: #ccd8e8%s, Alignment: Center);
                }
                """, !hasProperties ? "(0.4)" : ""));

        uiCommandBuilder.clear("#PropertiesDropdownContainer");
        uiCommandBuilder.appendInline("#PropertiesDropdownContainer", dropdown.toString());

        eventHandler.handle(CustomUIEventBindingType.ValueChanged,
                "#AddProperty",
                data -> data.append("@Value", "#AddProperty.Value"),
                this::onAddProperty,
                true
        );
    }

    private void onAddProperty(@Nonnull UIEventContext<EditorUI.Data> context) {
        state.commandsStack.execute(new AddPropertyCommand(state, context.data.value));
        state.ui.dirtyTimeline = true;
    }

}