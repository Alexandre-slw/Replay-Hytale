package gg.alexandre.replay.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.common.util.PathUtil;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import gg.alexandre.replay.ui.event.UIEventContext;
import gg.alexandre.replay.ui.event.UIEventHandler;
import gg.alexandre.replay.ui.event.UIEventIdData;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SaveUI extends BaseUI<SaveUI.Data> {

    public SaveUI(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, Data.get());
    }

    @Override
    public void init(@Nonnull UICommandBuilder uiCommandBuilder, @Nonnull UIEventBuilder uiEventBuilder,
                     @Nonnull UIEventHandler<Data> eventHandler) {
        uiCommandBuilder.append("SaveUI.ui");

        // TODO: set default name
        uiCommandBuilder.set("#Input.Value", "Test");

        eventHandler.handle(
                CustomUIEventBindingType.ValueChanged,
                "#Input",
                (data) -> data.append("@Input", "#Input.Value"),
                (context) -> {
                    System.out.println(context.data().value);
                }
        );

        eventHandler.handle(CustomUIEventBindingType.Activating,
                "#Discard",
                (_) -> {
                    System.out.println("Discard");
                }
        );

        eventHandler.handle(
                CustomUIEventBindingType.Activating,
                "#Save",
                (data) -> data.append("@Input", "#Input.Value"),
                (context) -> {
                    System.out.println("Save as " + context.data().value);
                }
        );
    }

    @Override
    public void handleEvent(@Nonnull UIEventContext<Data> context) {
        String value = context.data().value;
        if (value != null) {
            value = value.trim();

            // TODO: check if already exists
            boolean invalid = !PathUtil.isValidName(value);
            context.uiCommandBuilder().set("#Save.Disabled", invalid);
        }
    }

    public static class Data implements UIEventIdData {
        public static BuilderCodec.Builder<Data> get() {
            return BuilderCodec.builder(Data.class, Data::new)
                    .append(new KeyedCodec<>("@Input", Codec.STRING),
                            (data, v) -> data.value = v, data -> data.value
                    ).add();
        }

        private String value;
        private String eventId;

        @Nullable
        @Override
        public String getEventId() {
            return eventId;
        }

        @Override
        public void setEventId(@Nullable String eventId) {
            this.eventId = eventId;
        }
    }

}
