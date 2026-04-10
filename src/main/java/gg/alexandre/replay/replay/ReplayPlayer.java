package gg.alexandre.replay.replay;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.tick.TickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.packets.connection.Ping;
import com.hypixel.hytale.protocol.packets.entities.EntityUpdates;
import com.hypixel.hytale.protocol.packets.player.ClientReady;
import com.hypixel.hytale.protocol.packets.player.JoinWorld;
import com.hypixel.hytale.protocol.packets.setup.PlayerOptions;
import com.hypixel.hytale.protocol.packets.setup.RequestAssets;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.io.ServerManager;
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
import gg.alexandre.replay.repository.ReplayRepository;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class ReplayPlayer extends TickingSystem<EntityStore> {

    private final ReplayProtocol protocol;
    private final ReplayRepository repository;

    private final Map<UUID, ReplayState> states = new HashMap<>();

    private final HytaleLogger logger = HytaleLogger.forEnclosingClass();

    private final Gson gson = new Gson();

    public ReplayPlayer(@Nonnull ReplayProtocol protocol, @Nonnull ReplayRepository repository) {
        this.protocol = protocol;
        this.repository = repository;

        PacketAdapters.registerInbound((PacketFilter) (handler, packet) -> {
            ReplayState state = getState(handler);
            if (state == null) {
                return false;
            }

            assert handler.getAuth() != null;
            if (state.playerUuid == null || !state.playerUuid.equals(handler.getAuth().getUuid())) {
                return false;
            }

            if (packet instanceof RequestAssets) {
                state.file.consumeConfigPhase((replayPacket) -> replayPacket.handle(handler, state));
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

            if (packet instanceof PlayerOptions) {
                state.hasStarted = true;
            }

            if (packet instanceof ClientReady clientReady) {
                state.isProcessingPackets = clientReady.readyForChunks;
                state.isPlaying = clientReady.readyForGameplay;
            }

            return false;
        });

        PacketAdapters.registerOutbound((PacketFilter) (handler, packet) -> {
            ReplayState state = getState(handler);
            if (state == null) {
                return false;
            }

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

    @Nullable
    private ReplayState getState(@Nonnull PacketHandler packetHandler) {
        if (packetHandler.getAuth() == null) {
            return null;
        }

        return states.get(packetHandler.getAuth().getUuid());
    }

    private void clearWorld(@Nonnull PlayerRef playerRef) {
        Ref<EntityStore> ref = playerRef.getReference();
        assert ref != null;
        Store<EntityStore> store = ref.getStore();

        Player player = ref.getStore().getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

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

        playerRef.getPacketHandler().tryFlush();
    }

    public boolean isPlaying(@Nonnull PlayerRef playerRef) {
        ReplayState state = states.get(playerRef.getUuid());
        return state != null;
    }

    public void start(@Nonnull PlayerRef playerRef, @Nonnull String name) {
        ReplayState state = new ReplayState();
        state.playerUuid = playerRef.getUuid();

        try {
            state.file = new ReplayInputFile(repository.getReplay(playerRef, name), protocol);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        states.put(playerRef.getUuid(), state);

        try {
            // Some plugins might want to know that this is a replay
            JsonObject referral = new JsonObject();
            referral.addProperty("replay", true);

            InetSocketAddress publicAddress = ServerManager.get().getPublicAddress();
            assert publicAddress != null;

            playerRef.referToServer(
                    publicAddress.getHostName(),
                    publicAddress.getPort(),
                    gson.toJson(referral).getBytes(StandardCharsets.UTF_8)
            );
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }

    public void stop(@Nonnull PlayerRef playerRef) {
        ReplayState state = states.get(playerRef.getUuid());
        if (state == null || !state.hasStarted) {
            return;
        }

        states.remove(playerRef.getUuid());

        try {
            state.file.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.atInfo().log("Stopped replaying");

        if (playerRef.isValid()) {
            // TODO: transfer back?
            World world = playerRef.getReference().getStore().getExternalData().getWorld();
            world.execute(() -> {
                playerRef.removeFromStore();
                world.addPlayer(playerRef, null, true, false);
            });
        }
    }

    @Override
    public void tick(float v, int i, @Nonnull Store<EntityStore> store) {
        for (ReplayState state : states.values()) {
            tick(state);
        }
    }

    private void tick(@Nonnull ReplayState state) {
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
            } else {
                while (canProcessPackets(state, packetHandler) && state.file.read(state.tick)) {
                    state.file.consumePacket().handle(packetHandler, state);
                }
            }

            if (state.file.isEndOfFile()) {
                stop(playerRef);
            }
        } catch (Exception e) {
            logger.atWarning().withCause(e).log("Error while processing replay packets");
        } finally {
            state.isProcessingPackets = false;
        }

        if (canProcessPackets(state, packetHandler)) {
            state.tick++;
        }
    }

    private boolean canProcessPackets(@Nonnull ReplayState state, @Nonnull PacketHandler packetHandler) {
        if (!state.sentJoinWorld) {
            return true;
        }

        CompletableFuture<Void> clientReadyForChunksFuture = packetHandler.getClientReadyForChunksFuture();
        return clientReadyForChunksFuture == null || clientReadyForChunksFuture.isDone();
    }

}
