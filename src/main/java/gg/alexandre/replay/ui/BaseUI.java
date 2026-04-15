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

    private final UIEventHandler<T> eventHandler;

    public BaseUI(@Nonnull PlayerRef playerRef, @Nonnull CustomPageLifetime lifetime, @Nonnull BuilderCodec<T> codec) {
        super(playerRef, lifetime, codec);

        this.eventHandler = new UIEventHandler<>();
    }

    abstract public void init(@Nonnull UICommandBuilder uiCommandBuilder);

    abstract public void register(@Nonnull UIEventBuilder uiEventBuilder, @Nonnull UIEventHandler<T> eventHandler);

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder uiCommandBuilder,
                      @Nonnull UIEventBuilder uiEventBuilder, @Nonnull Store<EntityStore> store) {
        init(uiCommandBuilder);

        eventHandler.bind(uiEventBuilder);
        register(uiEventBuilder, eventHandler);
        eventHandler.bind(null);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull T data) {
        super.handleDataEvent(ref, store, data);

        UICommandBuilder uiCommandBuilder = new UICommandBuilder();
        UIEventContext<T> eventContext = new UIEventContext<>(ref, store, data, uiCommandBuilder);

        eventHandler.handleEvent(eventContext);
        handleEvent(eventContext);

        if (eventContext.isClosed()) {
            close();
        } else {
            sendUpdate(uiCommandBuilder, false);
        }
    }

    @Override
    public void handleEvent(@Nonnull UIEventContext<T> context) {

    }

}
