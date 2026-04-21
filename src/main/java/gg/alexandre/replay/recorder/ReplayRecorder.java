package gg.alexandre.replay.recorder;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.tick.TickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.CachedPacket;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.io.PacketIO;
import com.hypixel.hytale.protocol.io.PacketStatsRecorder;
import com.hypixel.hytale.protocol.packets.connection.Ping;
import com.hypixel.hytale.protocol.packets.player.ClientReady;
import com.hypixel.hytale.protocol.packets.player.JoinWorld;
import com.hypixel.hytale.protocol.packets.setup.WorldLoadFinished;
import com.hypixel.hytale.protocol.packets.setup.WorldLoadProgress;
import com.hypixel.hytale.protocol.packets.world.SpawnParticleSystem;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.AssetRegistryLoader;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PacketWatcher;
import com.hypixel.hytale.server.core.io.handlers.game.GamePacketHandler;
import com.hypixel.hytale.server.core.modules.i18n.I18nModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import gg.alexandre.replay.file.ReplayMetadata;
import gg.alexandre.replay.file.ReplayOutputFile;
import gg.alexandre.replay.protocol.ReplayPacket;
import gg.alexandre.replay.protocol.ReplayProtocol;
import gg.alexandre.replay.protocol.packets.HytaleReplayPacket;
import gg.alexandre.replay.repository.ReplayRepository;
import gg.alexandre.replay.ui.SaveUI;
import gg.alexandre.replay.util.DummyUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ReplayRecorder extends TickingSystem<EntityStore> {

    private final ReplayProtocol protocol;
    private final ReplayRepository repository;

    private final HytaleLogger logger = HytaleLogger.forEnclosingClass();

    private final Map<PlayerRef, RecordingData> recordings = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerRef> watcherToPlayer = new ConcurrentHashMap<>();

    public ReplayRecorder(@Nonnull ReplayProtocol protocol, @Nonnull ReplayRepository repository) {
        this.protocol = protocol;
        this.repository = repository;

        registerPacketsListener();
    }

    public void start(@Nonnull PlayerRef playerRef) {
        stop(playerRef);

        RecordingData data;
        try {
            data = new RecordingData(
                    new ReplayOutputFile(repository.newReplay(playerRef), protocol)
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        recordings.put(playerRef, data);

        String name = DummyUtil.NAME_PREFIX + playerRef.getUsername();
        UUID uuid = UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
        watcherToPlayer.put(uuid, playerRef);

        logger.atInfo().log("Started recording");

        Ref<EntityStore> ref = playerRef.getReference();
        assert ref != null;
        Store<EntityStore> store = ref.getStore();
        World world = store.getExternalData().getWorld();

        data.file.startSnapshot(data.tick);
        world.execute(() -> DummyUtil.spawnDummyWatcher(playerRef, name, uuid)
                .thenAccept(watcher -> {
                    data.file.endSnapshot(data.tick);

                    data.watcher = watcher;

                    data.file.configPhase(() -> {
                        PacketHandler packetHandler = watcher.getPacketHandler();
                        AssetRegistryLoader.sendAssets(packetHandler);
                        I18nModule.get().sendTranslations(packetHandler, watcher.getLanguage());
                        packetHandler.write(new WorldLoadProgress(
                                Message.translation(
                                        "client.general.worldLoad.loadingWorld"
                                ).getFormattedMessage(),
                                0,
                                0
                        ));
                        packetHandler.write(new WorldLoadFinished());
                    });
                }));
    }

    public void stop(@Nonnull PlayerRef playerRef) {
        RecordingData data = recordings.remove(playerRef);
        if (data == null) {
            return;
        }

        Ref<EntityStore> ref = playerRef.getReference();
        assert ref != null;
        Store<EntityStore> store = ref.getStore();
        World world = store.getExternalData().getWorld();
        world.execute(() -> {
            if (data.watcher != null) {
                watcherToPlayer.remove(data.watcher.getUuid());

                Ref<EntityStore> watcherRef = data.watcher.getReference();
                if (watcherRef != null && watcherRef.isValid()) {
                    world.getEntityStore().getStore().removeEntity(watcherRef, RemoveReason.REMOVE);
                }
            }
        });

        try {
            ReplayMetadata metadata = new ReplayMetadata(
                    data.start.until(Instant.now()).toMillis(),
                    data.tick
            );

            data.file.close(metadata);

            world.execute(() -> {
                Player player = store.getComponent(ref, Player.getComponentType());
                assert player != null;
                player.getPageManager().openCustomPage(ref, store, new SaveUI(playerRef, data.file.getSavePath()));
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.atInfo().log("Stopped recording");
    }

    public void stopAll() {
        recordings.keySet().forEach(this::stop);
    }

    @Override
    public void tick(float v, int i, @Nonnull Store<EntityStore> store) {
        for (RecordingData data : recordings.values()) {
            data.tick++;
        }
    }

    public void registerPacketsListener() {
        PacketAdapters.registerOutbound((PacketWatcher) (handler, packet) -> {
            if (packet instanceof Ping || handler.getAuth() == null) {
                return;
            }

            PlayerRef playerRef = watcherToPlayer.get(handler.getAuth().getUuid());
            if (playerRef == null) {
                return;
            }

            RecordingData data = recordings.get(playerRef);
            if (data == null) {
                return;
            }

            if (packet instanceof SpawnParticleSystem particleSystem &&
                    "PlayerSpawn_Spawn".equals(particleSystem.particleSystemId) &&
                    particleSystem.position != null &&
                    particleSystem.position.x == 0 &&
                    particleSystem.position.z == 0) {
                // Hide fake player spawn particles
                particleSystem.scale = 0;
                return;
            }

            if (packet instanceof JoinWorld && handler instanceof GamePacketHandler gamePacketHandler) {
                gamePacketHandler.handle(new ClientReady(true, false));
                gamePacketHandler.handle(new ClientReady(false, true));
            }

            data.file.write(toReplayPacket(packet), data.tick);
        });
    }

    @Nonnull
    private ReplayPacket toReplayPacket(@Nonnull Packet packet) {
        ByteBuf buffer = Unpooled.buffer();

        Class<? extends Packet> type = packet.getClass();
        if (packet instanceof CachedPacket<?> cachedPacket) {
            type = cachedPacket.getPacketType();
        }

        PacketIO.writeFramedPacket(packet, type, buffer, PacketStatsRecorder.NOOP);

        return new HytaleReplayPacket(buffer);
    }

}
