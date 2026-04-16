package gg.alexandre.replay.replay;

import com.google.gson.JsonObject;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.tick.TickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.packets.assets.UpdateTranslations;
import com.hypixel.hytale.protocol.packets.connection.Ping;
import com.hypixel.hytale.protocol.packets.entities.EntityUpdates;
import com.hypixel.hytale.protocol.packets.interaction.CancelInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.protocol.packets.interface_.CustomPage;
import com.hypixel.hytale.protocol.packets.interface_.ResetUserInterfaceState;
import com.hypixel.hytale.protocol.packets.interface_.SetPage;
import com.hypixel.hytale.protocol.packets.interface_.UpdateAnchorUI;
import com.hypixel.hytale.protocol.packets.player.ClientReady;
import com.hypixel.hytale.protocol.packets.player.JoinWorld;
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
import com.hypixel.hytale.server.core.modules.i18n.I18nModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import gg.alexandre.replay.ReplayPlugin;
import gg.alexandre.replay.file.ReplayInputFile;
import gg.alexandre.replay.protocol.ReplayProtocol;
import gg.alexandre.replay.replay.state.ReplayState;
import gg.alexandre.replay.ui.EditorUI;
import gg.alexandre.replay.ui.manager.RealtimePageManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class ReplayPlayer extends TickingSystem<EntityStore> {

    private final ReplayProtocol protocol;
    private final Map<UUID, ReplayState> states = new HashMap<>();
    private final HytaleLogger logger = HytaleLogger.forEnclosingClass();

    public ReplayPlayer(@Nonnull ReplayProtocol protocol) {
        this.protocol = protocol;

        PacketAdapters.registerInbound((PacketFilter) (handler, packet) -> {
            ReplayState state = getState(handler);
            if (state == null) {
                return false;
            }

            assert handler.getAuth() != null;
            if (state.playerUuid == null || !state.playerUuid.equals(handler.getAuth().getUuid())) {
                return false;
            }

            if (packet instanceof RequestAssets && !state.isFilteringPackets) {
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

            if (packet instanceof ClientReady clientReady) {
                state.isProcessingPackets = clientReady.readyForChunks;
                state.hasStarted = clientReady.readyForGameplay;
            }

            if (packet instanceof SyncInteractionChains syncInteractionChains) {
                return handleInteractionChains(handler, state, syncInteractionChains);
            }

            return false;
        });

        PacketAdapters.registerOutbound((PacketFilter) (handler, packet) -> {
            ReplayState state = getState(handler);
            if (state == null) {
                return false;
            }

            if (packet instanceof Ping ||
                packet instanceof SetPage ||
                packet instanceof CustomPage ||
                packet instanceof ResetUserInterfaceState ||
                packet instanceof UpdateAnchorUI) {
                return false;
            }

            if (packet instanceof UpdateTranslations && !state.sentTranslations) {
                state.sentTranslations = true;
                I18nModule.get().sendTranslations(handler, state.lang);
                return true;
            }

            if (packet instanceof JoinWorld && !state.sentJoinWorld) {
                state.sentJoinWorld = true;
                return false;
            }

            return state.isFilteringPackets && !state.isProcessingPackets;
        });
    }

    private boolean handleInteractionChains(@Nonnull PacketHandler handler, @Nonnull ReplayState state,
                                            @Nonnull SyncInteractionChains syncInteractionChains) {
        SyncInteractionChain interactionChain = null;
        int indexToRemove = -1;

        for (int i = 0; i < syncInteractionChains.updates.length; i++) {
            SyncInteractionChain chain = syncInteractionChains.updates[i];
            if (chain.interactionType == InteractionType.Primary && chain.initial) {
                interactionChain = chain;
                indexToRemove = i;
                break;
            }
        }

        if (interactionChain == null) {
            return false;
        }

        SyncInteractionChain chain = interactionChain;
        bypassFilter(state, () ->
                handler.writeNoCache(new CancelInteractionChain(chain.chainId, chain.forkedId))
        );
        state.controlGame = false;

        if (syncInteractionChains.updates.length == 1) {
            return true;
        }

        SyncInteractionChain[] updates = new SyncInteractionChain[syncInteractionChains.updates.length - 1];
        System.arraycopy(syncInteractionChains.updates, 0, updates, 0, indexToRemove);
        System.arraycopy(syncInteractionChains.updates, indexToRemove + 1, updates, indexToRemove,
                syncInteractionChains.updates.length - indexToRemove - 1);

        syncInteractionChains.updates = updates;
        return false;
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

    public void start(@Nonnull PlayerRef playerRef, @Nonnull Path replayPath) {
        ReplayState state = new ReplayState();
        state.replayPath = replayPath;
        state.playerUuid = playerRef.getUuid();
        state.lang = playerRef.getLanguage();
        // Run the first second so we don't see a blank world
        state.tick = 30;

        try {
            state.file = new ReplayInputFile(replayPath, protocol);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        states.put(playerRef.getUuid(), state);

        transfer(playerRef, true);
    }

    private static void transfer(@Nonnull PlayerRef playerRef, boolean replay) {
        try {
            byte[] referralData = null;
            if (replay) {
                // Some plugins might want to know that this is a replay
                JsonObject referral = new JsonObject();
                referral.addProperty("replay", true);

                referralData = ReplayPlugin.get().getGson().toJson(referral).getBytes(StandardCharsets.UTF_8);
            }

            InetSocketAddress publicAddress = ServerManager.get().getPublicAddress();
            assert publicAddress != null;

            playerRef.referToServer(
                    publicAddress.getHostName(),
                    publicAddress.getPort(),
                    referralData
            );
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }

    public void restart(@Nonnull PlayerRef playerRef) {
        ReplayState state = states.get(playerRef.getUuid());
        if (state == null || !state.hasStarted) {
            return;
        }

        try {
            state.file.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            state.file = new ReplayInputFile(state.replayPath, protocol);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        state.clearedWorld = false;
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
            transfer(playerRef, false);
        }
    }

    @Override
    public void tick(float v, int i, @Nonnull Store<EntityStore> store) {
        for (ReplayState state : states.values()) {
            tick(state);
        }
    }

    private void tick(@Nonnull ReplayState state) {
        if (!state.hasStarted) {
            return;
        }

        PlayerRef playerRef = Universe.get().getPlayer(state.playerUuid);
        if (playerRef == null || !playerRef.isValid()) {
            return;
        }

        handlePage(state, playerRef);

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

        if ((!state.sentJoinWorld || state.isPlaying) && canProcessPackets(state, packetHandler)) {
            state.tick++;
        }
    }

    private void handlePage(@Nonnull ReplayState state, @Nonnull PlayerRef playerRef) {
        if (state.controlGame) {
            return;
        }

        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null) {
            return;
        }

        Store<EntityStore> store = ref.getStore();
        Player player = store.getComponent(ref, Player.getComponentType());
        assert player != null;

        replacePageManager(playerRef, player);

        EditorUI editorUI;
        if (player.getPageManager().getCustomPage() instanceof EditorUI ui) {
            editorUI = ui;
        } else {
            editorUI = new EditorUI(playerRef, this, state);
            player.getPageManager().openCustomPage(ref, store, editorUI);
        }

        editorUI.tick();
    }

    private void replacePageManager(@Nonnull PlayerRef playerRef, @Nonnull Player player) {
        if (player.getPageManager() instanceof RealtimePageManager) {
            return;
        }

        try {
            Field pageManagerField = Player.class.getDeclaredField("pageManager");
            pageManagerField.setAccessible(true);

            pageManagerField.set(player, new RealtimePageManager(playerRef, player.getWindowManager()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean canProcessPackets(@Nonnull ReplayState state, @Nonnull PacketHandler packetHandler) {
        if (!state.sentJoinWorld) {
            return true;
        }

        CompletableFuture<Void> clientReadyForChunksFuture = packetHandler.getClientReadyForChunksFuture();
        return clientReadyForChunksFuture == null || clientReadyForChunksFuture.isDone();
    }

    public void bypassFilter(@Nonnull ReplayState state, @Nonnull Runnable runnable) {
        if (!state.isFilteringPackets) {
            runnable.run();
            return;
        }

        state.isFilteringPackets = false;
        try {
            runnable.run();
        } finally {
            state.isFilteringPackets = true;
        }
    }

}
