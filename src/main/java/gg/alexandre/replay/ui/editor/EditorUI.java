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
import gg.alexandre.replay.cutscene.CutSceneCodec;
import gg.alexandre.replay.replay.BasePlayer;
import gg.alexandre.replay.replay.state.ReplayState;
import gg.alexandre.replay.ui.BaseUI;
import gg.alexandre.replay.ui.CloseUI;
import gg.alexandre.replay.ui.CopyUI;
import gg.alexandre.replay.ui.HideUI;
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

        @UIKey("@Tick")
        public int tick;
    }

    private final BasePlayer player;
    private final ReplayState state;

    private boolean needToResume = false;

    private boolean helpVisible = false;

    private final Map<String, Object> cachedData = new HashMap<>();

    private final List<BaseRenderer<Data>> layoutRenderers;
    private final List<BaseRenderer<Data>> tickRenderers;

    public EditorUI(@Nonnull PlayerRef playerRef, BasePlayer player, ReplayState state) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, CODEC);
        this.player = player;
        this.state = state;

        KeyframesRenderer keyframesRenderer = new KeyframesRenderer(state, player);

        layoutRenderers = List.of(
                new PlayheadLayoutRenderer(state),
                new TimeScaleRenderer(state, player),
                keyframesRenderer
        );

        tickRenderers = List.of(
                new TimelinesDropdownRenderer(state, player),
                new PlaytailRenderer(state, player),
                new PropertiesDropdownRenderer(state),
                new PropertiesHeaderRenderer(state),
                keyframesRenderer,
                new PlayButtonRenderer(state)
        );
    }

    @Override
    public void init(@Nonnull UICommandBuilder uiCommandBuilder) {
        state.ui.dragging = false;

        uiCommandBuilder.append("Editor.ui");

        uiCommandBuilder.set("#Playhead.Max", player.getDurationTicks(state));

        uiCommandBuilder.set("#ExportCutScene.Visible", state.cutSceneMetadata != null);

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
                "#ZoomOut",
                this::onZoomOut
        );

        eventHandler.handle(CustomUIEventBindingType.Activating,
                "#ZoomIn",
                this::onZoomIn
        );

        eventHandler.handle(CustomUIEventBindingType.Activating,
                "#Save",
                this::onSave
        );

        eventHandler.handle(CustomUIEventBindingType.Activating,
                "#CloseButton",
                this::onClose
        );

        eventHandler.handle(CustomUIEventBindingType.Activating,
                "#Timeline",
                this::onClickContentBackground
        );

        eventHandler.handle(CustomUIEventBindingType.RightClicking,
                "#Timeline",
                this::onClickContentBackground
        );

        eventHandler.handle(CustomUIEventBindingType.Activating,
                "#Hide",
                this::onHide
        );

        eventHandler.handle(CustomUIEventBindingType.Activating,
                "#Undo",
                this::onUndo
        );

        eventHandler.handle(CustomUIEventBindingType.Activating,
                "#Redo",
                this::onRedo
        );

        eventHandler.handle(CustomUIEventBindingType.Activating,
                "#Help",
                this::onHelp
        );

        eventHandler.handle(CustomUIEventBindingType.Activating,
                "#ExportCutScene",
                this::onExportCutScene
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
        if (needToResume) {
            state.stage.isPlaying = true;
            needToResume = false;
        }

        if (context.data.playhead < state.targetTick) {
            player.restart(state);
        }

        state.targetTick = context.data.playhead;

        state.ui.draggingTick = context.data.playhead;
        state.ui.dragging = false;
    }

    private void onEsc(@Nonnull UIEventContext<Data> context) {
        state.ui.controlGame = true;
        state.ui.selectedKeyframe = null;
        context.close();
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

    private void onSave(@Nonnull UIEventContext<Data> context) {
        state.timeline.save(player.getSaveUUID(state), state.selectedTimeline);

        player.bypassFilter(state, () ->
                playerRef.sendMessage(
                        Message.translation("replay.saved").color(Color.GREEN)
                )
        );
    }

    private void onHide(@Nonnull UIEventContext<Data> context) {
        context.store.getExternalData().getWorld().execute(() -> {
            Player playerComponent = context.store.getComponent(context.ref, Player.getComponentType());
            assert playerComponent != null;
            playerComponent.getPageManager().openCustomPage(context.ref, context.store, new HideUI(playerRef));
        });
    }

    private void onUndo(@Nonnull UIEventContext<Data> context) {
        state.commandsStack.undo();
        state.ui.selectedKeyframe = null;
        state.ui.dirtyTimeline = true;
    }

    private void onRedo(@Nonnull UIEventContext<Data> context) {
        state.commandsStack.redo();
        state.ui.selectedKeyframe = null;
        state.ui.dirtyTimeline = true;
    }

    private void onHelp(@Nonnull UIEventContext<Data> context) {
        helpVisible = !helpVisible;
        context.uiCommandBuilder.set("#HelpPanel.Visible", helpVisible);
    }

    private void onExportCutScene(@Nonnull UIEventContext<Data> context) {
        context.store.getExternalData().getWorld().execute(() -> {
            Player playerComponent = context.store.getComponent(context.ref, Player.getComponentType());
            assert playerComponent != null;

            CutSceneCodec.Data data = new CutSceneCodec.Data(state.timeline, player.getDurationTicks(state));

            playerComponent.getPageManager().openCustomPage(
                    context.ref, context.store, new CopyUI(playerRef, CutSceneCodec.toDataString(data))
            );
        });
    }

    private void onClose(@Nonnull UIEventContext<Data> context) {
        context.store.getExternalData().getWorld().execute(() -> {
            Player playerComponent = context.store.getComponent(context.ref, Player.getComponentType());
            assert playerComponent != null;
            playerComponent.getPageManager().openCustomPage(context.ref, context.store, new CloseUI(playerRef));
        });
    }

    private void onClickContentBackground(@Nonnull UIEventContext<Data> context) {
        state.ui.selectedKeyframe = null;
        state.ui.dirtyTimeline = true;
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
        if (!state.ui.dragging) {
            update(uiCommandBuilder, "#Playhead.Value", (int) state.targetTick);
            state.ui.draggingTick = (int) state.targetTick;
        }

        update(uiCommandBuilder, "#Timelines.Value", state.selectedTimeline);

        update(uiCommandBuilder, "#Undo.Disabled", !state.commandsStack.canUndo());
        update(uiCommandBuilder, "#Redo.Disabled", !state.commandsStack.canRedo());

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
