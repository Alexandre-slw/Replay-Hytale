package gg.alexandre.replay.ui;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import gg.alexandre.replay.ReplayPlugin;
import gg.alexandre.replay.ui.codec.CodecConstructor;
import gg.alexandre.replay.ui.event.UIEventContext;
import gg.alexandre.replay.ui.event.UIEventHandler;
import gg.alexandre.replay.ui.event.UIEventIdData;

import javax.annotation.Nonnull;

public class CloseUI extends BaseUI<CloseUI.Data> {

    private static final BuilderCodec<Data> CODEC = CodecConstructor.create(Data.class, Data::new);

    public static class Data extends UIEventIdData {

    }

    public CloseUI(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, CODEC);
    }

    @Override
    public void init(@Nonnull UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.append("Close.ui");
    }

    @Override
    public void register(@Nonnull UIEventBuilder uiEventBuilder, @Nonnull UIEventHandler<Data> eventHandler) {
        eventHandler.handle(CustomUIEventBindingType.Activating,
                "#Discard",
                this::onDiscard
        );

        eventHandler.handle(
                CustomUIEventBindingType.Activating,
                "#Save",
                this::onSave
        );
    }

    private void onDiscard(@Nonnull UIEventContext<Data> context) {
        context.close();
    }

    private void onSave(@Nonnull UIEventContext<Data> context) {
        ReplayPlugin.get().stopReplaying(playerRef);
        ReplayPlugin.get().stopCutScene(playerRef);
        context.close();
    }

}
