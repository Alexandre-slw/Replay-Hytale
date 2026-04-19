package gg.alexandre.replay.events;

import com.hypixel.hytale.server.core.event.events.ShutdownEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import gg.alexandre.replay.ReplayPlugin;

import javax.annotation.Nonnull;

public class DisconnectEvent {

    public static void onPlayerDisconnect(@Nonnull PlayerDisconnectEvent event) {
        PlayerRef player = event.getPlayerRef();
        ReplayPlugin.get().stopRecording(player);
        ReplayPlugin.get().stopReplaying(player);
    }

    public static void onShutdown(@Nonnull ShutdownEvent event) {
        ReplayPlugin.get().stopAll();
    }

}