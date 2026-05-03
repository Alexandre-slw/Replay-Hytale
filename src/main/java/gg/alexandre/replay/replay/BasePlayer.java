package gg.alexandre.replay.replay;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.tick.TickingSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import gg.alexandre.replay.replay.state.ReplayState;
import gg.alexandre.replay.ui.editor.EditorUI;
import gg.alexandre.replay.ui.manager.RealtimePageManager;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.UUID;

public abstract class BasePlayer extends TickingSystem<EntityStore> {

    public void restart(@Nonnull ReplayState state) {
        state.currentTick = 0;
        state.stage.isPlaying = false;
    }

    public void bypassFilter(@Nonnull ReplayState state, @Nonnull Runnable runnable) {

    }

    protected void handlePage(@Nonnull ReplayState state, @Nonnull PlayerRef playerRef) {
        if (!state.useEditor || state.ui.controlGame) {
            return;
        }

        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null) {
            return;
        }

        Store<EntityStore> store = ref.getStore();
        Player player = store.getComponent(ref, Player.getComponentType());
        assert player != null;

        replacePageManager(playerRef, player);

        EditorUI editorUI;

        CustomUIPage customPage = player.getPageManager().getCustomPage();
        if (customPage instanceof EditorUI ui) {
            editorUI = ui;
        } else if (customPage == null) {
            editorUI = new EditorUI(playerRef, this, state);
            player.getPageManager().openCustomPage(ref, store, editorUI);
        } else {
            return;
        }

        editorUI.tick();
    }

    private void replacePageManager(@Nonnull PlayerRef playerRef, @Nonnull Player player) {
        if (player.getPageManager() instanceof RealtimePageManager) {
            return;
        }

        try {
            Field pageManagerField = Player.class.getDeclaredField("pageManager");
            pageManagerField.setAccessible(true);

            pageManagerField.set(player, new RealtimePageManager(playerRef, player.getWindowManager()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public abstract int getDurationTicks(@Nonnull ReplayState state);

    public abstract UUID getSaveUUID(@Nonnull ReplayState state);

}
