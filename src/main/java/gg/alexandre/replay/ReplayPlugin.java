package gg.alexandre.replay;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEffect;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.event.events.ShutdownEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.modules.entity.tracker.EntityTrackerSystems;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import gg.alexandre.replay.commands.ReplayCommand;
import gg.alexandre.replay.components.DummyViewerSystem;
import gg.alexandre.replay.components.TargetWatcherTag;
import gg.alexandre.replay.cutscene.CutScenePlayer;
import gg.alexandre.replay.events.DisconnectEvent;
import gg.alexandre.replay.events.WatcherConnectEvent;
import gg.alexandre.replay.gson.BasePropertyAdapter;
import gg.alexandre.replay.gson.PositionAdapter;
import gg.alexandre.replay.gson.SerializedNameOnlyStrategy;
import gg.alexandre.replay.protocol.ReplayProtocol;
import gg.alexandre.replay.recorder.ReplayRecorder;
import gg.alexandre.replay.replay.ReplayPlayer;
import gg.alexandre.replay.replay.editor.properties.base.BaseProperty;
import gg.alexandre.replay.replay.state.TimelineState;
import gg.alexandre.replay.repository.CutSceneRepository;
import gg.alexandre.replay.repository.ReplayRepository;
import gg.alexandre.replay.util.Position;
import gg.alexandre.replay.volumes.CutSceneEffect;

import javax.annotation.Nonnull;
import java.nio.file.Path;

public class ReplayPlugin extends JavaPlugin {

    public static ComponentType<EntityStore, TargetWatcherTag> TAG_TYPE;

    private static ReplayPlugin instance;

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Position.class, new PositionAdapter())
            .registerTypeAdapter(BaseProperty.class, new BasePropertyAdapter())
            .addDeserializationExclusionStrategy(new SerializedNameOnlyStrategy())
            .addSerializationExclusionStrategy(new SerializedNameOnlyStrategy())
            .create();

    private final ReplayProtocol protocol = new ReplayProtocol();
    private final ReplayRepository repository = new ReplayRepository(getDataDirectory());
    private final CutSceneRepository cutSceneRepository = new CutSceneRepository(getDataDirectory());

    private final ReplayRecorder recorder = new ReplayRecorder(protocol, repository);
    private final ReplayPlayer replayPlayer = new ReplayPlayer(protocol, getDataDirectory());
    private final CutScenePlayer cutScenePlayer = new CutScenePlayer();

    public ReplayPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    @Override
    protected void setup() {
        ComponentRegistryProxy<EntityStore> entityStoreRegistry = getEntityStoreRegistry();

        getCommandRegistry().registerCommand(new ReplayCommand("replay", "Replay commands"));
        getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, DisconnectEvent::onPlayerDisconnect);
        getEventRegistry().registerGlobal(ShutdownEvent.class, DisconnectEvent::onShutdown);
        getEventRegistry().registerGlobal(PlayerConnectEvent.class, WatcherConnectEvent::onPlayerConnect);

        entityStoreRegistry.registerSystem(replayPlayer);
        entityStoreRegistry.registerSystem(recorder);
        entityStoreRegistry.registerSystem(cutScenePlayer);

        TAG_TYPE = entityStoreRegistry.registerComponent(TargetWatcherTag.class, () -> {
            throw new UnsupportedOperationException();
        });

        replayPlayer.setup();

        TriggerEffect.CODEC.register("CutScene", CutSceneEffect.class, CutSceneEffect.CODEC);
    }

    @Override
    protected void start() {
        getEntityStoreRegistry().registerSystem(new DummyViewerSystem(
                EntityTrackerSystems.EntityViewer.getComponentType(), TAG_TYPE
        ));
    }

    public void startRecording(@Nonnull PlayerRef playerRef) {
        if (replayPlayer.isPlaying(playerRef)) {
            return;
        }

        recorder.start(playerRef);
    }

    public void stopRecording(@Nonnull PlayerRef playerRef) {
        recorder.stop(playerRef);
    }

    public void startReplaying(@Nonnull PlayerRef playerRef, @Nonnull Path replayPath) {
        recorder.stop(playerRef);
        replayPlayer.start(playerRef, replayPath);
    }

    public void stopReplaying(@Nonnull PlayerRef playerRef) {
        replayPlayer.stop(playerRef);
    }

    public void editCutScene(@Nonnull PlayerRef playerRef, @Nonnull Path path) {
        cutScenePlayer.start(playerRef, path);
    }

    public void startCutScene(@Nonnull PlayerRef playerRef, @Nonnull TimelineState timelineState) {
        cutScenePlayer.start(playerRef, timelineState);
    }

    public void stopCutScene(@Nonnull PlayerRef playerRef) {
        cutScenePlayer.stop(playerRef);
    }

    public void stopAll() {
        recorder.stopAll();
        replayPlayer.stopAll();
        cutScenePlayer.stopAll();
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
    public CutSceneRepository getCutSceneRepository() {
        return cutSceneRepository;
    }

    @Nonnull
    public ReplayRecorder getRecorder() {
        return recorder;
    }

    @Nonnull
    public ReplayPlayer getReplayPlayer() {
        return replayPlayer;
    }

    @Nonnull
    public Gson getGson() {
        return gson;
    }
}