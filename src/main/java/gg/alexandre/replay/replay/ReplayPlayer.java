package gg.alexandre.replay.replay;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.tick.TickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.packets.connection.Ping;
import com.hypixel.hytale.protocol.packets.entities.EntityUpdates;
import com.hypixel.hytale.protocol.packets.player.ClientReady;
import com.hypixel.hytale.protocol.packets.player.JoinWorld;
import com.hypixel.hytale.protocol.packets.setup.RequestAssets;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PacketFilter;
import com.hypixel.hytale.server.core.io.handlers.SetupPacketHandler;
import com.hypixel.hytale.server.core.modules.entity.player.ChunkTracker;
import com.hypixel.hytale.server.core.modules.entity.tracker.EntityTrackerSystems;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import gg.alexandre.replay.file.ReplayInputFile;
import gg.alexandre.replay.protocol.ReplayProtocol;
import gg.alexandre.replay.replay.state.ReplayState;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class ReplayPlayer extends TickingSystem<EntityStore> {

    private ReplayState state = new ReplayState();

    private ReplayInputFile inputFile;

    private final HytaleLogger logger = HytaleLogger.forEnclosingClass();

    private final ReplayProtocol protocol;

    public ReplayPlayer(ReplayProtocol protocol) {
        this.protocol = protocol;

        PacketAdapters.registerInbound((PacketFilter) (handler, packet) -> {
            if (state.playerUuid == null ||
                    handler.getAuth() == null ||
                    !state.playerUuid.equals(handler.getAuth().getUuid())) {
                return false;
            }

            if (packet instanceof RequestAssets) {
                inputFile.consumeConfigPhase((replayPacket) -> replayPacket.handle(handler, state));
                handler.tryFlush();

                try {
                    Field receivedRequest = SetupPacketHandler.class.getDeclaredField("receivedRequest");
                    receivedRequest.setAccessible(true);
                    receivedRequest.set(handler, true);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                state.isFilteringPackets = true;

                return true;
            }

            if (packet instanceof ClientReady clientReady) {
                state.isProcessingPackets = clientReady.readyForChunks;
                state.isPlaying = clientReady.readyForGameplay;
            }

            return false;
        });
        PacketAdapters.registerOutbound((PacketFilter) (_, packet) -> {
            if (packet instanceof Ping) {
                return false;
            }

            if (packet instanceof JoinWorld && !state.sentJoinWorld) {
                state.sentJoinWorld = true;
                return false;
            }

            return state.isFilteringPackets && !state.isProcessingPackets;
        });
    }

    private void clearWorld(PlayerRef playerRef) {
        Ref<EntityStore> ref = playerRef.getReference();
        Store<EntityStore> store = ref.getStore();

        Player player = ref.getStore().getComponent(ref, Player.getComponentType());
        if (player != null) {
            ChunkTracker tracker = store.getComponent(ref, ChunkTracker.getComponentType());
            if (tracker != null) {
                tracker.unloadAll(playerRef);
            }

            EntityTrackerSystems.EntityViewer viewer = store.getComponent(ref, EntityTrackerSystems.EntityViewer.getComponentType());
            if (viewer != null) {
                EntityUpdates entityUpdates = new EntityUpdates();

                List<Integer> visibleEntities = viewer.visible.stream()
                        .map(entity -> entity.getStore().getComponent(entity, NetworkId.getComponentType()))
                        .filter(Objects::nonNull)
                        .map(NetworkId::getId)
                        .toList();

                entityUpdates.removed = new int[visibleEntities.size()];
                for (int i = 0; i < visibleEntities.size(); i++) {
                    entityUpdates.removed[i] = visibleEntities.get(i);
                }

                playerRef.getPacketHandler().write(entityUpdates);
            }
        }
    }

    public void start() {
        try {
            // TODO: use repository
            inputFile = new ReplayInputFile(Path.of("test.replay"), protocol);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        state = new ReplayState();

        // TODO: setup only for requesting user
        for (PlayerRef playerRef : Universe.get().getPlayers()) {
            state.playerUuid = playerRef.getUuid();
            // TODO: use real address and port
            playerRef.referToServer("localhost", 5520);
            break;
        }
    }

    public CompletableFuture<Void> stop() {
        if (inputFile == null) {
            return CompletableFuture.completedFuture(null);
        }

        try {
            inputFile.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.atInfo().log("Stopped replaying");

        CompletableFuture<Void> future = new CompletableFuture<>();

        PlayerRef playerRef = Universe.get().getPlayer(state.playerUuid);
        if (playerRef != null && playerRef.isValid()) {
            World world = playerRef.getReference().getStore().getExternalData().getWorld();
            world.execute(() -> {
                playerRef.removeFromStore();
                CompletableFuture<PlayerRef> playerFuture = world.addPlayer(
                        playerRef, null, true, false
                );
                if (playerFuture != null) {
                    playerFuture.thenRun(() -> future.complete(null));
                }
            });
        }

        state = new ReplayState();

        return future;
    }

    @Override
    public void tick(float v, int i, @Nonnull Store<EntityStore> store) {
        if (!state.isPlaying) {
            return;
        }

        PlayerRef playerRef = Universe.get().getPlayer(state.playerUuid);
        if (playerRef == null || !playerRef.isValid()) {
            return;
        }

        PacketHandler packetHandler = playerRef.getPacketHandler();
        try {
            state.isProcessingPackets = true;

            if (!state.clearedWorld) {
                clearWorld(playerRef);
                state.clearedWorld = true;
            }

            while (canProcessPackets(packetHandler) && inputFile.read(state.tick)) {
                inputFile.consumePacket().handle(packetHandler, state);
            }

            if (inputFile.isEndOfFile()) {
                stop();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            state.isProcessingPackets = false;
        }

        if (canProcessPackets(packetHandler)) {
            state.tick++;
        }
    }

    private boolean canProcessPackets(PacketHandler packetHandler) {
        if (!state.sentJoinWorld) {
            return true;
        }

        CompletableFuture<Void> clientReadyForChunksFuture = packetHandler.getClientReadyForChunksFuture();
        return clientReadyForChunksFuture == null || clientReadyForChunksFuture.isDone();
    }

}
