package gg.alexandre.replay.ui;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import gg.alexandre.replay.file.ReplayMetadata;
import gg.alexandre.replay.replay.ReplayPlayer;
import gg.alexandre.replay.replay.state.ReplayState;
import gg.alexandre.replay.ui.codec.CodecConstructor;
import gg.alexandre.replay.ui.codec.UIKey;
import gg.alexandre.replay.ui.event.UIEventContext;
import gg.alexandre.replay.ui.event.UIEventHandler;
import gg.alexandre.replay.ui.event.UIEventIdData;

import javax.annotation.Nonnull;

public class EditorUI extends BaseUI<EditorUI.Data> {

    private static final BuilderCodec<Data> CODEC = CodecConstructor.create(Data.class, Data::new);

    public static class Data extends UIEventIdData {
        @UIKey("@Playhead")
        int playhead;
    }

    private final ReplayPlayer player;
    private final ReplayState state;

    public EditorUI(@Nonnull PlayerRef playerRef, ReplayPlayer player, ReplayState state) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, CODEC);
        this.player = player;
        this.state = state;
    }

    @Override
    public void init(@Nonnull UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.append("EditorUI.ui");

        ReplayMetadata metadata = state.file.getMetadata();
        uiCommandBuilder.set("#Playhead.Max", metadata.ticks);
    }

    @Override
    public void register(@Nonnull UIEventBuilder uiEventBuilder, @Nonnull UIEventHandler<Data> eventHandler) {
        eventHandler.handle(CustomUIEventBindingType.Activating,
                "#Pause",
                this::onPause
        );

        eventHandler.handle(CustomUIEventBindingType.ValueChanged,
                "#Playhead",
                data -> data.append("@Playhead", "#Playhead.Value"),
                this::onPlayhead
        );
    }

    private void onPlayhead(@Nonnull UIEventContext<Data> context) {
        player.restart(playerRef);
        state.tick = context.data().playhead;
        close();
    }

    private void onPause(@Nonnull UIEventContext<Data> context) {
        state.isPlaying = !state.isPlaying;
        close();
    }

}
