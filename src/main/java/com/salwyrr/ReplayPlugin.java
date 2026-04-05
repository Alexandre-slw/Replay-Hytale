package com.salwyrr;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.modules.entity.tracker.EntityTrackerSystems;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.salwyrr.commands.ReplayCommand;
import com.salwyrr.components.DummyReceiver;
import com.salwyrr.components.DummyViewerSystem;
import com.salwyrr.components.TargetWatcherTag;
import com.salwyrr.events.ExampleEvent;
import com.salwyrr.protocol.ReplayProtocol;
import com.salwyrr.recorder.ReplayRecorder;
import com.salwyrr.replay.ReplayPlayer;
import com.salwyrr.repository.ReplayRepository;

import javax.annotation.Nonnull;

public class ReplayPlugin extends JavaPlugin {

    public static ComponentType<EntityStore, TargetWatcherTag> TAG_TYPE;

    private static ReplayPlugin instance;

    private final ReplayProtocol protocol = new ReplayProtocol();
    private final ReplayRepository repository = new ReplayRepository();

    private final ReplayRecorder recorder = new ReplayRecorder(protocol);
    private final ReplayPlayer player = new ReplayPlayer(protocol);

    public ReplayPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    @Override
    protected void setup() {
        ComponentRegistryProxy<EntityStore> entityStoreRegistry = getEntityStoreRegistry();

        getCommandRegistry().registerCommand(new ReplayCommand("replay", "Replay commands"));
        getEventRegistry().registerGlobal(PlayerReadyEvent.class, ExampleEvent::onPlayerReady);

        entityStoreRegistry.registerSystem(player);
        entityStoreRegistry.registerSystem(recorder);

        TAG_TYPE = entityStoreRegistry.registerComponent(TargetWatcherTag.class, () -> {
            throw new UnsupportedOperationException();
        });
    }

    @Override
    protected void start() {
        getEntityStoreRegistry().registerSystem(new DummyViewerSystem(
                EntityTrackerSystems.EntityViewer.getComponentType(), TAG_TYPE
        ));
    }

    public static Ref<EntityStore> spawnDummyWatcher(World world, Ref<EntityStore> targetPlayer) {
        Store<EntityStore> store = world.getEntityStore().getStore();

        Holder<EntityStore> holder = store.getRegistry().newHolder();

        holder.putComponent(
                EntityTrackerSystems.EntityViewer.getComponentType(),
                new EntityTrackerSystems.EntityViewer(0, new DummyReceiver())
        );
        holder.putComponent(TAG_TYPE, new TargetWatcherTag(targetPlayer));

        Ref<EntityStore> dummyRef = store.addEntity(holder, AddReason.SPAWN);

        if (dummyRef == null || !dummyRef.isValid()) {
            throw new RuntimeException("Failed to spawn dummy watcher");
        }

        return dummyRef;
    }

    public void startRecording() {
        player.stop();
        recorder.start();
    }

    public void stopRecording() {
        recorder.stop();
    }

    public void startReplaying() {
        recorder.stop();
        player.start();
    }

    public void stopReplaying() {
        player.stop();
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

    public ReplayProtocol getProtocol() {
        return protocol;
    }

    public static ReplayPlugin get() {
        return instance;
    }
}