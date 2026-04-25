package gg.alexandre.replay.ui;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import gg.alexandre.replay.ui.event.UIEventConsumer;
import gg.alexandre.replay.ui.event.UIEventContext;
import gg.alexandre.replay.ui.event.UIEventHandler;
import gg.alexandre.replay.ui.event.UIEventIdData;

import javax.annotation.Nonnull;

public abstract class BaseUI<T extends UIEventIdData> extends InteractiveCustomUIPage<T> implements UIEventConsumer<T> {

    protected final UIEventHandler<T> eventHandler;

    public BaseUI(@Nonnull PlayerRef playerRef, @Nonnull CustomPageLifetime lifetime, @Nonnull BuilderCodec<T> codec) {
        super(playerRef, lifetime, codec);

        this.eventHandler = new UIEventHandler<>();
    }

    abstract public void init(@Nonnull UICommandBuilder uiCommandBuilder);

    abstract public void register(@Nonnull UIEventBuilder uiEventBuilder, @Nonnull UIEventHandler<T> eventHandler);

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder uiCommandBuilder,
                      @Nonnull UIEventBuilder uiEventBuilder, @Nonnull Store<EntityStore> store) {
        eventHandler.bind(uiEventBuilder);
        init(uiCommandBuilder);
        register(uiEventBuilder, eventHandler);
        eventHandler.unbind();
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull T data) {
        super.handleDataEvent(ref, store, data);

        Ref<EntityStore> reference = playerRef.getReference();
        if (reference == null || !reference.isValid()) {
            return;
        }

        Store<EntityStore> playerStore = reference.getStore();
        playerStore.getExternalData().getWorld().execute(() -> {
            if (!reference.isValid()) {
                return;
            }

            UICommandBuilder uiCommandBuilder = new UICommandBuilder();
            UIEventBuilder uiEventBuilder = new UIEventBuilder();
            eventHandler.bind(uiEventBuilder);

            UIEventContext<T> eventContext = new UIEventContext<>(
                    playerRef, ref, playerStore, data, uiCommandBuilder, eventHandler
            );

            eventHandler.handleEvent(eventContext);
            handleEvent(eventContext);

            eventHandler.unbind();

            if (eventContext.isClosed()) {
                close();
            } else if (uiCommandBuilder.getCommands().length > 0) {
                if (uiEventBuilder.getEvents().length > 0) {
                    sendUpdate(uiCommandBuilder, uiEventBuilder, false);
                } else {
                    sendUpdate(uiCommandBuilder);
                }
            } else if (uiEventBuilder.getEvents().length > 0) {
                sendUpdate(null, uiEventBuilder, false);
            }
        });
    }

    @Override
    public void handleEvent(@Nonnull UIEventContext<T> context) {

    }

}
