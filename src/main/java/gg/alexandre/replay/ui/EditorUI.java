package gg.alexandre.replay.ui;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import gg.alexandre.replay.file.ReplayMetadata;
import gg.alexandre.replay.replay.ReplayPlayer;
import gg.alexandre.replay.replay.state.ReplayState;
import gg.alexandre.replay.ui.codec.CodecConstructor;
import gg.alexandre.replay.ui.codec.UIKey;
import gg.alexandre.replay.ui.event.UIEventContext;
import gg.alexandre.replay.ui.event.UIEventHandler;
import gg.alexandre.replay.ui.event.UIEventIdData;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class EditorUI extends BaseUI<EditorUI.Data> {

    private static final BuilderCodec<Data> CODEC = CodecConstructor.create(Data.class, Data::new);

    public static class Data extends UIEventIdData {
        @UIKey("@Playhead")
        int playhead;
    }

    private final ReplayPlayer player;
    private final ReplayState state;

    private boolean needToResume = false;

    private boolean pauseUpdates = false;

    private final Map<String, Object> cachedData = new HashMap<>();

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

        tick(uiCommandBuilder);
    }

    @Override
    public void register(@Nonnull UIEventBuilder uiEventBuilder, @Nonnull UIEventHandler<Data> eventHandler) {
        eventHandler.handle(CustomUIEventBindingType.ValueChanged,
                "#Playhead",
                data -> data.append("@Playhead", "#Playhead.Value"),
                this::onPlayhead
        );

        eventHandler.handle(CustomUIEventBindingType.MouseButtonReleased,
                "#Playhead",
                data -> data.append("@Playhead", "#Playhead.Value"),
                this::onPlayheadRelease
        );

        eventHandler.handle(CustomUIEventBindingType.ValueChanged,
                "#Esc",
                this::onEsc
        );

        eventHandler.handle(CustomUIEventBindingType.MouseButtonReleased,
                "#Esc",
                this::onEsc
        );

        eventHandler.handle(CustomUIEventBindingType.Activating,
                "#Pause",
                this::onPause
        );
    }

    private void onPlayhead(@Nonnull UIEventContext<Data> context) {
        if (context.data.playhead > state.tick) {
            state.tick = context.data.playhead;
        }

        if (state.isPlaying) {
            state.isPlaying = false;
            needToResume = true;
        }

        pauseUpdates = true;
    }

    private void onPlayheadRelease(@Nonnull UIEventContext<Data> context) {
        if (context.data.playhead < state.tick) {
            player.restart(playerRef);
        }

        state.tick = context.data.playhead;

        if (needToResume) {
            state.isPlaying = true;
            needToResume = false;
        }

        pauseUpdates = false;
    }

    private void onEsc(@Nonnull UIEventContext<Data> context) {
        state.controlGame = true;
        close();
    }

    private void onPause(@Nonnull UIEventContext<Data> context) {
        state.isPlaying = !state.isPlaying;
    }

    public void tick() {
        if (pauseUpdates) {
            return;
        }

        UICommandBuilder uiCommandBuilder = new UICommandBuilder();
        tick(uiCommandBuilder);

        if (uiCommandBuilder.getCommands().length == 0) {
            return;
        }

        sendUpdate(uiCommandBuilder, false);
    }

    public void tick(@Nonnull UICommandBuilder uiCommandBuilder) {
        update(uiCommandBuilder,"#Pause.KeyBindingLabel",
                Message.translation(state.isPlaying ? "replay.pause" : "replay.play"));
//        update(uiCommandBuilder,"#Mask.Visible", state.isPlaying);
        update(uiCommandBuilder, "#Playhead.Value", state.tick);
    }

    private void update(@Nonnull UICommandBuilder uiCommandBuilder, @Nonnull String selector, @Nonnull Object value) {
        Object cachedValue = cachedData.get(selector);
        if (cachedValue != null) {
            if (cachedValue.equals(value)) {
                return;
            }

            if (cachedValue instanceof Message cachedMessage && value instanceof Message message) {
                if (cachedMessage.getMessageId() == null || message.getMessageId() == null) {
                    throw new IllegalStateException("Message IDs cannot be null for caching");
                }

                if (cachedMessage.getMessageId().equals(message.getMessageId())) {
                    return;
                }
            }
        }

        switch (value) {
            case Integer v -> uiCommandBuilder.set(selector, v);
            case String v -> uiCommandBuilder.set(selector, v);
            case Boolean v -> uiCommandBuilder.set(selector, v);
            case Message v -> uiCommandBuilder.set(selector, v);
            default -> throw new IllegalArgumentException("Unsupported value type: " + value.getClass());
        }

        cachedData.put(selector, value);
    }

    @Override
    public void onDismiss(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        if (!state.controlGame) {
            // TODO: exit prompt
        } else if (!state.sentEscHint) {
            state.sentEscHint = true;

            player.bypassFilter(state, () ->
                    playerRef.sendMessage(
                            Message.translation("replay.leftClickToReopenEditor").color(Color.CYAN)
                    )
            );
        }
    }
}
