package gg.alexandre.replay.ui.event;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public final class UIEventContext<T extends UIEventIdData> {

    @Nonnull
    public final Ref<EntityStore> ref;
    @Nonnull
    public final Store<EntityStore> store;
    @Nonnull
    public final T data;
    @Nonnull
    public final UICommandBuilder uiCommandBuilder;

    private boolean closed;

    public UIEventContext(@Nonnull Ref<EntityStore> ref,
                          @Nonnull Store<EntityStore> store,
                          @Nonnull T data,
                          @Nonnull UICommandBuilder uiCommandBuilder) {
        this.ref = ref;
        this.store = store;
        this.data = data;
        this.uiCommandBuilder = uiCommandBuilder;
    }

    public void close() {
        this.closed = true;
    }

    public boolean isClosed() {
        return closed;
    }
}
