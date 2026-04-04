package com.salwyrr;

import com.salwyrr.recorder.ReplayRecorder;
import com.salwyrr.replay.ReplayPlayer;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.salwyrr.commands.ReplayCommand;
import com.salwyrr.events.ExampleEvent;
import com.salwyrr.repository.ReplayRepository;

import javax.annotation.Nonnull;

public class ReplayPlugin extends JavaPlugin {

    private static ReplayPlugin instance;

    private final ReplayRecorder recorder = new ReplayRecorder();
    private final ReplayPlayer player = new ReplayPlayer();
    private final ReplayRepository repository = new ReplayRepository();

    public ReplayPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    @Override
    protected void setup() {
        this.getCommandRegistry().registerCommand(new ReplayCommand("replay", "Replay commands"));
        this.getEventRegistry().registerGlobal(PlayerReadyEvent.class, ExampleEvent::onPlayerReady);

        recorder.registerPacketCounters();
        this.getEntityStoreRegistry().registerSystem(player);
    }

    public ReplayRecorder getRecorder() {
        return recorder;
    }

    public ReplayPlayer getPlayer() {
        return player;
    }

    public ReplayRepository getRepository() {
        return repository;
    }

    public static ReplayPlugin get() {
        return instance;
    }
}