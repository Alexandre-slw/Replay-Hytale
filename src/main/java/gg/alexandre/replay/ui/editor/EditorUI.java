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
import gg.alexandre.replay.ui.NewTimelineUI;
import gg.alexandre.replay.ui.codec.CodecConstructor;
import gg.alexandre.replay.ui.codec.UIKey;
import gg.alexandre.replay.ui.common.CommonUI;
import gg.alexandre.replay.ui.editor.renderers.BaseRenderer;
import gg.alexandre.replay.ui.editor.renderers.PlayheadRenderer;
import gg.alexandre.replay.ui.editor.renderers.TimeScaleRenderer;
import gg.alexandre.replay.ui.event.UIEventContext;
import gg.alexandre.replay.ui.event.UIEventHandler;
import gg.alexandre.replay.ui.event.UIEventIdData;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditorUI extends BaseUI<EditorUI.Data> {

    private static final BuilderCodec<Data> CODEC = CodecConstructor.create(Data.class, Data::new);
    private static final int WIDTH = 1200;

    public static class Data extends UIEventIdData {
        @UIKey("@Playhead")
        int playhead;
        @UIKey("@Timeline")
        String timeline;
    }

    private final ReplayPlayer player;
    private final ReplayState state;

    private boolean needToResume = false;

    private boolean pauseUpdates = false;

    private final Map<String, Object> cachedData = new HashMap<>();
    private List<String> cachedTimelines = new ArrayList<>();

    private final List<BaseRenderer> renderers = List.of(
                new PlayheadRenderer(),
                new TimeScaleRenderer()
    );

    public EditorUI(@Nonnull PlayerRef playerRef, ReplayPlayer player, ReplayState state) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, CODEC);
        this.player = player;
        this.state = state;
    }

    @Override
    public void init(@Nonnull UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.append("Editor.ui");

        ReplayMetadata metadata = state.file.getMetadata();
        uiCommandBuilder.set("#Playhead.Max", metadata.ticks);

        layout(uiCommandBuilder);
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

        eventHandler.handle(CustomUIEventBindingType.Activating,
                "#ZoomOut",
                this::onZoomOut
        );

        eventHandler.handle(CustomUIEventBindingType.Activating,
                "#ZoomIn",
                this::onZoomIn
        );

        eventHandler.handle(CustomUIEventBindingType.ValueChanged,
                "#Timelines",
                data -> data.append("@Timeline", "#Timelines.Value"),
                this::onTimelineSelected
        );

        eventHandler.handle(CustomUIEventBindingType.Activating,
                "#Close",
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

        pauseUpdates = true;
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

        pauseUpdates = false;
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
        layout(context.uiCommandBuilder);
    }

    private void onZoomIn(@Nonnull UIEventContext<Data> context) {
        if (state.ui.timelineZoom >= 4f) {
            return;
        }

        state.ui.timelineZoom *= 1.5f;
        layout(context.uiCommandBuilder);
    }

    private void onTimelineSelected(@Nonnull UIEventContext<Data> context) {
        String selected = context.data.timeline;
        if (selected.equals(state.selectedTimeline)) {
            return;
        }

        if (selected.equals("/newTimeline")) {
            Ref<EntityStore> ref = context.ref;
            Store<EntityStore> store = context.store;
            Player playerComponent = store.getComponent(ref, Player.getComponentType());
            assert playerComponent != null;
            playerComponent.getPageManager().openCustomPage(ref, store, new NewTimelineUI(playerRef, state));
        } else {
            state.loadTimeline(selected);
            context.close();
        }
    }

    private void onClose(@Nonnull UIEventContext<Data> context) {
        context.store.getExternalData().getWorld().execute(() -> {
            Player playerComponent = context.store.getComponent(context.ref, Player.getComponentType());
            assert playerComponent != null;
            playerComponent.getPageManager().openCustomPage(context.ref, context.store, new CloseUI(playerRef));
        });
    }

    public void layout(@Nonnull UICommandBuilder uiCommandBuilder) {
        int width = (int) (WIDTH * state.ui.timelineZoom);
        for (BaseRenderer renderer : renderers) {
            renderer.layout(uiCommandBuilder, state, width);
        }
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
        update(uiCommandBuilder, "#Pause.KeyBindingLabel",
                Message.translation(state.stage.isPlaying ? "replay.pause" : "replay.play"));
        update(uiCommandBuilder, "#Timelines.Value", state.selectedTimeline);
        update(uiCommandBuilder, "#Playhead.Value", (int) state.targetTick);

        updateTimelinesDropdown(uiCommandBuilder);
    }

    private void updateTimelinesDropdown(@Nonnull UICommandBuilder uiCommandBuilder) {
        if (cachedTimelines.size() != state.timelines.size()) {
            StringBuilder dropdown = new StringBuilder(CommonUI.DEFAULT_DROPDOWN_STYLE);

            dropdown.append(String.format("""
                    DropdownBox #Timelines {
                      Anchor: (Width: 150, Height: 26);
                      Style: (...@DefaultDropdownBoxStyle, EntriesInViewport: 4, ArrowWidth: 0);
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

            uiCommandBuilder.remove("#Timelines");
            uiCommandBuilder.appendInline("#TimelinesContainer", dropdown.toString());

            cachedTimelines = new ArrayList<>(state.timelines);
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
