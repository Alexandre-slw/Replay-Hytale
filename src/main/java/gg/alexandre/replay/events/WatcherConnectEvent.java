package gg.alexandre.replay.events;

import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.RemovedPlayerFromWorldEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import gg.alexandre.replay.ReplayPlugin;
import gg.alexandre.replay.util.CameramanUtil;

import javax.annotation.Nonnull;

public class WatcherConnectEvent {

    public static void onPlayerConnect(@Nonnull PlayerConnectEvent event) {
        World world = ReplayPlugin.get().getRecorder().getWorldForWatcher(event.getPlayerRef());

        if (world != null) {
            event.setWorld(world);
        }
    }

    public static void onAddPlayerToWorld(@Nonnull AddPlayerToWorldEvent event) {
        PlayerRef playerRef = event.getHolder().getComponent(PlayerRef.getComponentType());
        if (playerRef == null || !playerRef.getUsername().startsWith(CameramanUtil.NAME_PREFIX)) {
            return;
        }

        event.setBroadcastJoinMessage(false);
        event.setJoinMessage(null);
    }

    public static void onRemovedPlayerFromWorld(@Nonnull RemovedPlayerFromWorldEvent event) {
        PlayerRef playerRef = event.getHolder().getComponent(PlayerRef.getComponentType());
        if (playerRef == null || !playerRef.getUsername().startsWith(CameramanUtil.NAME_PREFIX)) {
            return;
        }

        event.setBroadcastLeaveMessage(false);
        event.setLeaveMessage(null);
    }

}