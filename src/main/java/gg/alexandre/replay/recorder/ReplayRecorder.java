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
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PacketWatcher;
import com.hypixel.hytale.server.core.io.handlers.game.GamePacketHandler;
import com.hypixel.hytale.server.core.modules.i18n.I18nModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import gg.alexandre.replay.file.ReplayOutputFile;
import gg.alexandre.replay.protocol.ReplayPacket;
import gg.alexandre.replay.protocol.ReplayProtocol;
import gg.alexandre.replay.protocol.packets.HytaleReplayPacket;
import gg.alexandre.replay.repository.ReplayRepository;
import gg.alexandre.replay.util.DummyUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ReplayRecorder extends TickingSystem<EntityStore> {

    private final ReplayProtocol protocol;
    private final ReplayRepository repository;

    private final HytaleLogger logger = HytaleLogger.forEnclosingClass();

    private boolean recording;
    private ReplayOutputFile outputFile;
    private int tick = 0;

    private Map<PlayerRef, Ref<EntityStore>> watchers = new HashMap<>();

    public ReplayRecorder(@Nonnull ReplayProtocol protocol, @Nonnull ReplayRepository repository) {
        this.protocol = protocol;
        this.repository = repository;

        registerPacketsListener();
    }

    public void start(@Nonnull PlayerRef playerRef) {
        stop(playerRef);

        try {
            outputFile = new ReplayOutputFile(repository.newReplay(playerRef), protocol);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.atInfo().log("Started recording");

        tick = 0;
        recording = true;

        Ref<EntityStore> ref = playerRef.getReference();
        Store<EntityStore> store = ref.getStore();
        World world = store.getExternalData().getWorld();

        outputFile.startSnapshot(tick);
        world.execute(() -> DummyUtil.spawnDummyWatcher(playerRef)
                .thenAccept(watcher -> {
                    outputFile.endSnapshot(tick);

                    watchers.put(playerRef, watcher.getReference());

                    outputFile.configPhase(() -> {
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
        if (!recording) {
            return;
        }

        recording = false;

        for (PlayerRef player : Universe.get().getPlayers()) {
            Ref<EntityStore> ref = player.getReference();
            Store<EntityStore> store = ref.getStore();
            World world = store.getExternalData().getWorld();
            world.execute(() -> {
                Ref<EntityStore> watcherRef = watchers.remove(player);
                if (watcherRef != null) {
                    world.getEntityStore().getStore().removeEntity(watcherRef, RemoveReason.REMOVE);
                }
            });
        }

        try {
            outputFile.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.atInfo().log("Stopped recording");
    }

    @Override
    public void tick(float v, int i, @Nonnull Store<EntityStore> store) {
        if (!recording) {
            return;
        }

        tick++;
    }

    public void registerPacketsListener() {
        // TODO: record based on player
        PacketAdapters.registerOutbound((PacketWatcher) (handler, packet) -> {
            if (!recording) {
                return;
            }

            if (packet instanceof Ping) {
                return;
            }

            if (packet instanceof SpawnParticleSystem particleSystem &&
                    particleSystem.particleSystemId.equals("PlayerSpawn_Spawn") &&
                    particleSystem.position.x == 0 &&
                    particleSystem.position.z == 0) {
                // Hide dummy player spawn particles
                particleSystem.scale = 0;
                return;
            }

            if (!(handler instanceof GamePacketHandler gamePacketHandler)) {
                return;
            }

            if (packet instanceof JoinWorld) {
                gamePacketHandler.handle(new ClientReady(true, false));
                gamePacketHandler.handle(new ClientReady(false, true));
            }

            // TODO: filter by recording user
            PlayerRef ref = gamePacketHandler.getPlayerRef();
            boolean isDummy = ref.getUsername().startsWith("DummyPlayer_");

            if (!isDummy) {
                return;
            }

            outputFile.write(toReplayPacket(packet), tick);
        });
    }

    @Nonnull
    private ReplayPacket toReplayPacket(@Nonnull Packet packet) {
        ByteBuf buffer = Unpooled.buffer(packet.computeSize() + 256);

        Class<? extends Packet> type = packet.getClass();
        if (packet instanceof CachedPacket<?> cachedPacket) {
            type = cachedPacket.getPacketType();
        }

        PacketIO.writeFramedPacket(packet, type, buffer, PacketStatsRecorder.NOOP);

        return new HytaleReplayPacket(buffer);
    }

}
