package gg.alexandre.replay.events;

import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.universe.world.World;
import gg.alexandre.replay.ReplayPlugin;

import javax.annotation.Nonnull;

public class WatcherConnectEvent {

    public static void onPlayerConnect(@Nonnull PlayerConnectEvent event) {
        World world = ReplayPlugin.get().getRecorder().getWorldForWatcher(event.getPlayerRef());

        if (world != null) {
            event.setWorld(world);
        }
    }

}