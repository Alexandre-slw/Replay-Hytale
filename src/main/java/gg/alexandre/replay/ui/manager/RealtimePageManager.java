package gg.alexandre.replay.ui.manager;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageEvent;
import com.hypixel.hytale.server.core.entity.entities.player.pages.PageManager;
import com.hypixel.hytale.server.core.entity.entities.player.windows.WindowManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class RealtimePageManager extends PageManager {

    public RealtimePageManager(@Nonnull PlayerRef playerRef, @Nonnull WindowManager windowManager) {
        init(playerRef, windowManager);
    }

    @Override
    public void handleEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                            @Nonnull CustomPageEvent event) {
        switch (event.type) {
            case Dismiss:
                super.handleEvent(ref, store, event);
                break;

            case Data:
                this.clearCustomPageAcknowledgements();
                super.handleEvent(ref, store, event);
                break;

            case Acknowledge:
                this.clearCustomPageAcknowledgements();
                break;
        }
    }
}
