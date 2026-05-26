package gg.alexandre.replay.ui;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import gg.alexandre.replay.ui.codec.CodecConstructor;
import gg.alexandre.replay.ui.codec.UIKey;
import gg.alexandre.replay.ui.event.UIEventContext;
import gg.alexandre.replay.ui.event.UIEventHandler;
import gg.alexandre.replay.ui.event.UIEventIdData;

import javax.annotation.Nonnull;

public class CopyUI extends BaseUI<CopyUI.Data> {

    private final String data;

    private static final BuilderCodec<Data> CODEC = CodecConstructor.create(Data.class, Data::new);

    public static class Data extends UIEventIdData {
        @UIKey("@Input")
        private String value;
    }

    public CopyUI(@Nonnull PlayerRef playerRef, @Nonnull String data) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, CODEC);

        this.data = data;
    }

    @Override
    public void init(@Nonnull UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.append("Copy.ui");

        uiCommandBuilder.set("#Input.Value", data);
    }

    @Override
    public void register(@Nonnull UIEventBuilder uiEventBuilder, @Nonnull UIEventHandler<Data> eventHandler) {
        eventHandler.handle(
                CustomUIEventBindingType.Activating,
                "#Save",
                (data) -> data.append("@Input", "#Input.Value"),
                this::onSave
        );
    }

    private void onSave(@Nonnull UIEventContext<Data> context) {
        context.close();
    }

}
