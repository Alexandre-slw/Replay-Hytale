package gg.alexandre.replay.ui.event;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public record UIEventContext<T extends UIEventIdData>(@Nonnull Ref<EntityStore> ref,
                                                      @Nonnull Store<EntityStore> store,
                                                      @Nonnull T data,
                                                      @Nonnull UICommandBuilder uiCommandBuilder) {
}
