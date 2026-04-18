package gg.alexandre.replay.ui;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.common.util.PathUtil;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import gg.alexandre.replay.replay.state.ReplayState;
import gg.alexandre.replay.ui.codec.CodecConstructor;
import gg.alexandre.replay.ui.codec.UIKey;
import gg.alexandre.replay.ui.event.UIEventContext;
import gg.alexandre.replay.ui.event.UIEventHandler;
import gg.alexandre.replay.ui.event.UIEventIdData;

import javax.annotation.Nonnull;

public class NewTimelineUI extends BaseUI<NewTimelineUI.Data> {

    private final ReplayState state;

    private static final BuilderCodec<Data> CODEC = CodecConstructor.create(Data.class, Data::new);

    public static class Data extends UIEventIdData {
        @UIKey("@Input")
        private String value;
    }

    public NewTimelineUI(@Nonnull PlayerRef playerRef, @Nonnull ReplayState state) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, CODEC);

        this.state = state;
    }

    @Override
    public void init(@Nonnull UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.append("NewTimeline.ui");

        String name = "Unnamed";
        int i = 1;
        while (state.timelines.contains(name)) {
            name = "Unnamed " + i;
            i++;
        }

        uiCommandBuilder.set("#Input.Value", name);
    }

    @Override
    public void register(@Nonnull UIEventBuilder uiEventBuilder, @Nonnull UIEventHandler<Data> eventHandler) {
        eventHandler.handle(
                CustomUIEventBindingType.ValueChanged,
                "#Input",
                (data) -> data.append("@Input", "#Input.Value"),
                this::onInputChange
        );

        eventHandler.handle(CustomUIEventBindingType.Activating,
                "#Discard",
                this::onDiscard
        );

        eventHandler.handle(
                CustomUIEventBindingType.Activating,
                "#Save",
                (data) -> data.append("@Input", "#Input.Value"),
                this::onSave
        );
    }

    private void onInputChange(@Nonnull UIEventContext<Data> context) {
        String value = context.data.value;
        if (value != null) {
            value = value.trim();
            context.uiCommandBuilder.set("#Save.Disabled", isInvalidPath(value));
        }
    }

    private void onDiscard(@Nonnull UIEventContext<Data> context) {
        context.close();
    }

    private void onSave(@Nonnull UIEventContext<Data> context) {
        String value = context.data.value;
        if (value == null) {
            return;
        }

        value = value.trim();
        if (isInvalidPath(value)) {
            return;
        }

        state.loadTimeline(value);
        context.close();
    }

    private boolean isInvalidPath(@Nonnull String value) {
        return !PathUtil.isValidName(value) || state.timelines.contains(value);
    }

}
