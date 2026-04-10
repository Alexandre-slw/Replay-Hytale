package gg.alexandre.replay.events;

import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import gg.alexandre.replay.ReplayPlugin;

public class ExampleEvent {

    public static void onPlayerDisconnect(PlayerDisconnectEvent event) {
        PlayerRef player = event.getPlayerRef();
        ReplayPlugin.get().stopReplaying(player);
    }

}