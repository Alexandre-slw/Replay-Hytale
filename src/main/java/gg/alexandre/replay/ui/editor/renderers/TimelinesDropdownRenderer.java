package gg.alexandre.replay.ui.editor.renderers;

import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import gg.alexandre.replay.replay.state.ReplayState;
import gg.alexandre.replay.ui.common.CommonUI;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class TimelinesDropdownRenderer extends BaseRenderer {

    private List<String> cachedTimelines = new ArrayList<>();

    @Override
    public void render(@Nonnull UICommandBuilder uiCommandBuilder, @Nonnull ReplayState state, int width) {
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
    }

}