package gg.alexandre.replay;

import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.modules.entity.tracker.EntityTrackerSystems;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import gg.alexandre.replay.commands.ReplayCommand;
import gg.alexandre.replay.components.DummyViewerSystem;
import gg.alexandre.replay.components.TargetWatcherTag;
import gg.alexandre.replay.events.ExampleEvent;
import gg.alexandre.replay.protocol.ReplayProtocol;
import gg.alexandre.replay.recorder.ReplayRecorder;
import gg.alexandre.replay.replay.ReplayPlayer;
import gg.alexandre.replay.repository.ReplayRepository;

import javax.annotation.Nonnull;

public class ReplayPlugin extends JavaPlugin {

    public static ComponentType<EntityStore, TargetWatcherTag> TAG_TYPE;

    private static ReplayPlugin instance;

    private final ReplayProtocol protocol = new ReplayProtocol();
    private final ReplayRepository repository = new ReplayRepository(getDataDirectory());

    private final ReplayRecorder recorder = new ReplayRecorder(protocol, repository);
    private final ReplayPlayer player = new ReplayPlayer(protocol, repository);

    public ReplayPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    @Override
    protected void setup() {
        ComponentRegistryProxy<EntityStore> entityStoreRegistry = getEntityStoreRegistry();

        getCommandRegistry().registerCommand(new ReplayCommand("replay", "Replay commands"));
        getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, ExampleEvent::onPlayerDisconnect);

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

    public void startRecording(PlayerRef playerRef) {
        if (player.isPlaying(playerRef)) {
            return;
        }

        recorder.start(playerRef);
    }

    public void stopRecording(PlayerRef playerRef) {
        recorder.stop(playerRef);
    }

    public void startReplaying(PlayerRef playerRef, String name) {
        recorder.stop(playerRef);
        player.start(playerRef, name);
    }

    public void stopReplaying(PlayerRef playerRef) {
        player.stop(playerRef);
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