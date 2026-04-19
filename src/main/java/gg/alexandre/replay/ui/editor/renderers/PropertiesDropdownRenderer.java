package gg.alexandre.replay.ui.editor.renderers;

import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import gg.alexandre.replay.replay.editor.registry.PropertyRegistry;
import gg.alexandre.replay.replay.state.ReplayState;
import gg.alexandre.replay.ui.common.CommonUI;
import gg.alexandre.replay.ui.editor.EditorUI;
import gg.alexandre.replay.ui.event.UIEventContext;
import gg.alexandre.replay.ui.event.UIEventHandler;

import javax.annotation.Nonnull;

public class PropertiesDropdownRenderer extends BaseRenderer<EditorUI.Data> {

    private int propertiesCount = -1;

    public PropertiesDropdownRenderer(ReplayState state) {
        super(state);
    }

    @Override
    public void render(@Nonnull UICommandBuilder uiCommandBuilder, @Nonnull UIEventHandler<EditorUI.Data> eventHandler,
                       @Nonnull ReplayState state, int width) {
        int count = PropertyRegistry.get().getRegistry().size();
        if (propertiesCount == count) {
            return;
        }
        propertiesCount = count;

        StringBuilder dropdown = new StringBuilder(CommonUI.DEFAULT_DROPDOWN_STYLE);

        dropdown.append("""
                DropdownBox #AddProperty {
                  Anchor: (Full: 0);
                  Style: (
                    ...@DefaultDropdownBoxStyle,
                    EntriesInViewport: 3,
                    ArrowWidth: 0,
                    LabelStyle: (TextColor: #000000(0))
                  );
                  Value: "";
                """);

        for (String id : PropertyRegistry.get().getRegistry().keySet()) {
            dropdown.append(String.format("""
                    DropdownEntry {
                      Value: "%s";
                      Text: %%replay.%s;
                    }
                    """, id, id));
        }

        dropdown.append("}");

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
        // TODO: undo/redo
        state.timeline.getProperties().add(PropertyRegistry.get().create(context.data.value));
    }

}