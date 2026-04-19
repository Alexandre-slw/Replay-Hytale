package gg.alexandre.replay.ui.editor.renderers;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import gg.alexandre.replay.replay.state.ReplayState;
import gg.alexandre.replay.ui.NewTimelineUI;
import gg.alexandre.replay.ui.common.CommonUI;
import gg.alexandre.replay.ui.editor.EditorUI;
import gg.alexandre.replay.ui.event.UIEventContext;
import gg.alexandre.replay.ui.event.UIEventHandler;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class TimelinesDropdownRenderer extends BaseRenderer<EditorUI.Data> {

    private List<String> cachedTimelines = new ArrayList<>();

    public TimelinesDropdownRenderer(ReplayState state) {
        super(state);
    }

    @Override
    public void render(@Nonnull UICommandBuilder uiCommandBuilder, @Nonnull UIEventHandler<EditorUI.Data> eventHandler,
                       @Nonnull ReplayState state, int width) {
        if (cachedTimelines.size() == state.timelines.size()) {
            return;
        }

        cachedTimelines = new ArrayList<>(state.timelines);

        StringBuilder dropdown = new StringBuilder(CommonUI.DEFAULT_DROPDOWN_STYLE);

        dropdown.append(String.format("""
                DropdownBox #Timelines {
                  Anchor: (Width: 150, Height: 26);
                  TooltipText: %%replay.selectedTimeline;
                  Style: (
                    ...@DefaultDropdownBoxStyle,
                    EntriesInViewport: 4,
                    ArrowWidth: 0,
                    LabelStyle: (TextColor: #ccd8e8, Alignment: Center)
                  );
                  Value: "%s";
                """, state.selectedTimeline));

        for (String timeline : state.timelines) {
            dropdown.append(String.format("""
                    DropdownEntry {
                      Value: "%s";
                      Text: "%s";
                    }
                    """, timeline, timeline));
        }

        dropdown.append("""
                  DropdownEntry {
                    Value: "/newTimeline";
                    Text: %replay.plusNewTimeline;
                  }
                }
                """);

        uiCommandBuilder.clear("#TimelinesContainer");
        uiCommandBuilder.appendInline("#TimelinesContainer", dropdown.toString());

        eventHandler.handle(CustomUIEventBindingType.ValueChanged,
                "#Timelines",
                data -> data.append("@Value", "#Timelines.Value"),
                this::onTimelineSelected
        );
    }

    private void onTimelineSelected(@Nonnull UIEventContext<EditorUI.Data> context) {
        String selected = context.data.value;
        if (selected.equals(state.selectedTimeline)) {
            return;
        }

        if (selected.equals("/newTimeline")) {
            Ref<EntityStore> ref = context.ref;
            Store<EntityStore> store = context.store;
            Player playerComponent = store.getComponent(ref, Player.getComponentType());
            assert playerComponent != null;
            playerComponent.getPageManager().openCustomPage(ref, store, new NewTimelineUI(context.playerRef, state));
        } else {
            state.loadTimeline(selected);
            context.close();
        }
    }

}