package gg.alexandre.replay.ui.editor;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import gg.alexandre.replay.file.ReplayMetadata;
import gg.alexandre.replay.replay.ReplayPlayer;
import gg.alexandre.replay.replay.state.ReplayState;
import gg.alexandre.replay.ui.BaseUI;
import gg.alexandre.replay.ui.CloseUI;
import gg.alexandre.replay.ui.codec.CodecConstructor;
import gg.alexandre.replay.ui.codec.UIKey;
import gg.alexandre.replay.ui.editor.renderers.*;
import gg.alexandre.replay.ui.event.UIEventContext;
import gg.alexandre.replay.ui.event.UIEventHandler;
import gg.alexandre.replay.ui.event.UIEventIdData;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditorUI extends BaseUI<EditorUI.Data> {

    private static final BuilderCodec<Data> CODEC = CodecConstructor.create(Data.class, Data::new);
    private static final int WIDTH = 1200;

    public static class Data extends UIEventIdData {
        @UIKey("@Playhead")
        public int playhead;

        @UIKey("@Value")
        public String value;
    }

    private final ReplayPlayer player;
    private final ReplayState state;

    private boolean needToResume = false;

    private final Map<String, Object> cachedData = new HashMap<>();

    private final List<BaseRenderer<Data>> layoutRenderers;
    private final List<BaseRenderer<Data>> tickRenderers;

    public EditorUI(@Nonnull PlayerRef playerRef, ReplayPlayer player, ReplayState state) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, CODEC);
        this.player = player;
        this.state = state;

        layoutRenderers = List.of(
                new PlayheadLayoutRenderer(state),
                new TimeScaleRenderer(state)
        );

        tickRenderers = List.of(
                new TimelinesDropdownRenderer(state),
                new PlaytailRenderer(state),
                new PropertiesDropdownRenderer(state),
                new PropertiesHeaderRenderer(state)
        );
    }

    @Override
    public void init(@Nonnull UICommandBuilder uiCommandBuilder) {
        state.ui.dragging = false;

        uiCommandBuilder.append("Editor.ui");

        ReplayMetadata metadata = state.file.getMetadata();
        uiCommandBuilder.set("#Playhead.Max", metadata.ticks);

        layout(uiCommandBuilder, eventHandler);
        tick(uiCommandBuilder, eventHandler);
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

        eventHandler.handle(CustomUIEventBindingType.Activating,
                "#ZoomOut",
                this::onZoomOut
        );

        eventHandler.handle(CustomUIEventBindingType.Activating,
                "#ZoomIn",
                this::onZoomIn
        );

        eventHandler.handle(CustomUIEventBindingType.Activating,
                "#CloseButton",
                this::onClose
        );
    }

    private void onPlayhead(@Nonnull UIEventContext<Data> context) {
        if (context.data.playhead > state.targetTick) {
            state.targetTick = context.data.playhead;
        }

        if (state.stage.isPlaying) {
            state.stage.isPlaying = false;
            needToResume = true;
        }

        state.ui.draggingTick = context.data.playhead;
        state.ui.dragging = true;
    }

    private void onPlayheadRelease(@Nonnull UIEventContext<Data> context) {
        if (context.data.playhead < state.targetTick) {
            player.restart(playerRef);
        }

        state.targetTick = context.data.playhead;

        if (needToResume) {
            state.stage.isPlaying = true;
            needToResume = false;
        }

        state.ui.draggingTick = context.data.playhead;
        state.ui.dragging = false;
    }

    private void onEsc(@Nonnull UIEventContext<Data> context) {
        state.ui.controlGame = true;
        context.close();
    }

    private void onPause(@Nonnull UIEventContext<Data> context) {
        state.stage.isPlaying = !state.stage.isPlaying;
    }

    private void onZoomOut(@Nonnull UIEventContext<Data> context) {
        if (state.ui.timelineZoom <= 0.45f) {
            return;
        }

        state.ui.timelineZoom /= 1.5f;

        layout(context.uiCommandBuilder, eventHandler);
    }

    private void onZoomIn(@Nonnull UIEventContext<Data> context) {
        if (state.ui.timelineZoom >= 4f) {
            return;
        }

        state.ui.timelineZoom *= 1.5f;

        layout(context.uiCommandBuilder, eventHandler);
    }

    private void onClose(@Nonnull UIEventContext<Data> context) {
        context.store.getExternalData().getWorld().execute(() -> {
            Player playerComponent = context.store.getComponent(context.ref, Player.getComponentType());
            assert playerComponent != null;
            playerComponent.getPageManager().openCustomPage(context.ref, context.store, new CloseUI(playerRef));
        });
    }

    public void layout(@Nonnull UICommandBuilder uiCommandBuilder, @Nonnull UIEventHandler<Data> eventHandler) {
        int width = (int) (WIDTH * state.ui.timelineZoom);
        for (BaseRenderer<Data> renderer : layoutRenderers) {
            renderer.render(uiCommandBuilder, eventHandler, state, width);
        }
    }

    public void tick() {
        UICommandBuilder uiCommandBuilder = new UICommandBuilder();
        UIEventBuilder uiEventBuilder = new UIEventBuilder();

        eventHandler.bind(uiEventBuilder);
        tick(uiCommandBuilder, eventHandler);
        eventHandler.unbind();

        if (uiCommandBuilder.getCommands().length == 0 && uiEventBuilder.getEvents().length == 0) {
            return;
        }

        sendUpdate(uiCommandBuilder, uiEventBuilder, false);
    }

    public void tick(@Nonnull UICommandBuilder uiCommandBuilder, @Nonnull UIEventHandler<Data> eventHandler) {
        update(uiCommandBuilder, "#Pause.KeyBindingLabel",
                Message.translation(state.stage.isPlaying ? "replay.pause" : "replay.play"));

        if (!state.ui.dragging) {
            update(uiCommandBuilder, "#Playhead.Value", (int) state.targetTick);
            state.ui.draggingTick = (int) state.targetTick;
        }

        update(uiCommandBuilder, "#Timelines.Value", state.selectedTimeline);

        int width = (int) (WIDTH * state.ui.timelineZoom);
        for (BaseRenderer<Data> renderer : tickRenderers) {
            renderer.render(uiCommandBuilder, eventHandler, state, width);
        }
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
            case Double v -> uiCommandBuilder.set(selector, v);
            case String v -> uiCommandBuilder.set(selector, v);
            case Boolean v -> uiCommandBuilder.set(selector, v);
            case Message v -> uiCommandBuilder.set(selector, v);
            default -> throw new IllegalArgumentException("Unsupported value type: " + value.getClass());
        }

        cachedData.put(selector, value);
    }

    @Override
    public void onDismiss(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        if (state.ui.controlGame && !state.ui.sentEscHint) {
            state.ui.sentEscHint = true;

            player.bypassFilter(state, () ->
                    playerRef.sendMessage(
                            Message.translation("replay.leftClickToReopenEditor").color(Color.CYAN)
                    )
            );
        }
    }

}
