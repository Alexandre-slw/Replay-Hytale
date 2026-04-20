package gg.alexandre.replay.ui;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import gg.alexandre.replay.ui.codec.CodecConstructor;
import gg.alexandre.replay.ui.event.UIEventContext;
import gg.alexandre.replay.ui.event.UIEventHandler;
import gg.alexandre.replay.ui.event.UIEventIdData;

import javax.annotation.Nonnull;

public class HideUI extends BaseUI<HideUI.Data> {

    private static final BuilderCodec<Data> CODEC = CodecConstructor.create(Data.class, Data::new);

    public static class Data extends UIEventIdData {

    }

    public HideUI(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, CODEC);
    }

    @Override
    public void init(@Nonnull UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.append("Hide.ui");
    }

    @Override
    public void register(@Nonnull UIEventBuilder uiEventBuilder, @Nonnull UIEventHandler<Data> eventHandler) {
        eventHandler.handle(CustomUIEventBindingType.ValueChanged,
                "#Esc",
                UIEventContext::close);

        eventHandler.handle(CustomUIEventBindingType.MouseButtonReleased,
                "#Esc",
                UIEventContext::close);
    }

}
