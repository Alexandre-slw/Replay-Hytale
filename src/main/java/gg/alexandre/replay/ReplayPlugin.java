package gg.alexandre.replay;

import com.google.gson.Gson;
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
import gg.alexandre.replay.events.DisconnectEvent;
import gg.alexandre.replay.protocol.ReplayProtocol;
import gg.alexandre.replay.recorder.ReplayRecorder;
import gg.alexandre.replay.replay.ReplayPlayer;
import gg.alexandre.replay.repository.ReplayRepository;

import javax.annotation.Nonnull;
import java.nio.file.Path;

public class ReplayPlugin extends JavaPlugin {

    public static ComponentType<EntityStore, TargetWatcherTag> TAG_TYPE;

    private static ReplayPlugin instance;

    private final ReplayProtocol protocol = new ReplayProtocol();
    private final ReplayRepository repository = new ReplayRepository(getDataDirectory());

    private final ReplayRecorder recorder = new ReplayRecorder(protocol, repository);
    private final ReplayPlayer player = new ReplayPlayer(protocol);

    private final Gson gson = new Gson();

    public ReplayPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    @Override
    protected void setup() {
        ComponentRegistryProxy<EntityStore> entityStoreRegistry = getEntityStoreRegistry();

        getCommandRegistry().registerCommand(new ReplayCommand("replay", "Replay commands"));
        getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, DisconnectEvent::onPlayerDisconnect);

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

    public void startRecording(@Nonnull PlayerRef playerRef) {
        if (player.isPlaying(playerRef)) {
            return;
        }

        recorder.start(playerRef);
    }

    public void stopRecording(@Nonnull PlayerRef playerRef) {
        recorder.stop(playerRef);
    }

    public void startReplaying(@Nonnull PlayerRef playerRef, @Nonnull Path replayPath) {
        recorder.stop(playerRef);
        player.start(playerRef, replayPath);
    }

    public void stopReplaying(@Nonnull PlayerRef playerRef) {
        player.stop(playerRef);
    }

    @Nonnull
    public static ReplayPlugin get() {
        return instance;
    }

    @Nonnull
    public ReplayRepository getRepository() {
        return repository;
    }

    @Nonnull
    public Gson getGson() {
        return gson;
    }
}