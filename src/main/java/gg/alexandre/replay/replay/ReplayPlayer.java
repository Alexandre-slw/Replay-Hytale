package gg.alexandre.replay.replay;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.tick.TickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.*;
import com.hypixel.hytale.protocol.packets.assets.UpdateBlockHitboxes;
import com.hypixel.hytale.protocol.packets.assets.UpdateTranslations;
import com.hypixel.hytale.protocol.packets.camera.SetServerCamera;
import com.hypixel.hytale.protocol.packets.connection.ClientDisconnect;
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
import com.hypixel.hytale.protocol.packets.player.ClientTeleport;
import com.hypixel.hytale.protocol.packets.player.JoinWorld;
import com.hypixel.hytale.protocol.packets.player.SetMovementStates;
import com.hypixel.hytale.protocol.packets.setup.RequestAssets;
import com.hypixel.hytale.protocol.packets.setup.SetTimeDilation;
import com.hypixel.hytale.protocol.packets.setup.WorldLoadFinished;
import com.hypixel.hytale.protocol.packets.setup.WorldLoadProgress;
import com.hypixel.hytale.server.core.Constants;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.io.ServerManager;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PacketFilter;
import com.hypixel.hytale.server.core.io.handlers.SetupPacketHandler;
import com.hypixel.hytale.server.core.modules.entity.player.ChunkTracker;
import com.hypixel.hytale.server.core.modules.i18n.I18nModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.PositionUtil;
import gg.alexandre.replay.file.ReplayInputFile;
import gg.alexandre.replay.protocol.ReplayPacket;
import gg.alexandre.replay.protocol.ReplayProtocol;
import gg.alexandre.replay.protocol.packets.TickReplayPacket;
import gg.alexandre.replay.replay.editor.properties.base.BaseProperty;
import gg.alexandre.replay.replay.state.ReplayState;
import gg.alexandre.replay.ui.editor.EditorUI;
import gg.alexandre.replay.ui.manager.RealtimePageManager;
import gg.alexandre.replay.util.Position;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ReplayPlayer extends TickingSystem<EntityStore> {

    private static final AtomicInteger NEXT_TELEPORT_ID = new AtomicInteger();

    private final ReplayProtocol protocol;
    private final Map<UUID, ReplayState> states = new ConcurrentHashMap<>();
    private final HytaleLogger logger = HytaleLogger.forEnclosingClass();

    public ReplayPlayer(@Nonnull ReplayProtocol protocol, @Nonnull Path dataDirectory) {
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

            if (packet instanceof SyncInteractionChains syncInteractionChains) {
                return handleInteractionChains(handler, state, syncInteractionChains);
            }

            if (state.stage.isFilteringPackets && packet instanceof ClientDisconnect) {
                stop(state);
            }

            return false;
        });

        PacketAdapters.registerOutbound((PacketFilter) (handler, packet) -> {
            ReplayState state = getState(handler);
            if (state == null) {
                return false;
            }

            if (packet instanceof JoinWorld) {
                return true;
            }

            if (packet instanceof Ping ||
                    packet instanceof SetPage ||
                    packet instanceof CustomPage ||
                    packet instanceof ResetUserInterfaceState ||
                    packet instanceof UpdateAnchorUI) {

                return packet instanceof CustomPage customPage &&
                       customPage.key != null && !customPage.key.startsWith("gg.alexandre.");
            }

            if (!state.stage.isFilteringPackets && packet instanceof ToClientPacket toClientPacket) {
                if (packet instanceof WorldLoadProgress || packet instanceof WorldLoadFinished) {
                    return true;
                }

                handleUpdatePackets(toClientPacket);
            }

            if (packet instanceof UpdateTranslations && !state.stage.sentTranslations) {
                state.stage.sentTranslations = true;
                I18nModule.get().sendTranslations(handler, state.lang);
                return true;
            }

            boolean filter = state.stage.isFilteringPackets && !state.stage.isProcessingPackets;

            if (packet instanceof EntityUpdates entityUpdates) {
                if (!filter && entityUpdates.removed != null) {
                    for (int id : entityUpdates.removed) {
                        state.entityIds.remove(id);
                    }
                }

                if (entityUpdates.updates != null) {
                    for (EntityUpdate update : entityUpdates.updates) {
                        state.entityIds.add(update.networkId);
                    }
                }
            }

            return filter;
        });
    }

    private void handleUpdatePackets(@Nonnull ToClientPacket packet) {
        Class<? extends ToClientPacket> packetClass = packet.getClass();
        for (Field field : packetClass.getDeclaredFields()) {
            if (field.getType().isAssignableFrom(UpdateType.class)) {
                try {
                    field.setAccessible(true);
                    field.set(packet, UpdateType.AddOrUpdate);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        if (packet instanceof UpdateBlockHitboxes hitboxes) {
            assert hitboxes.blockBaseHitboxes != null;
            hitboxes.blockBaseHitboxes.replaceAll((_, _) -> new Hitbox[0]);
        }
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
        state.ui.controlGame = false;

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

    private void clearWorld(@Nonnull PlayerRef playerRef, @Nonnull ReplayState state) {
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

        PacketHandler packetHandler = playerRef.getPacketHandler();

        EntityUpdates entityUpdates = new EntityUpdates();
        entityUpdates.removed = state.entityIds.stream().mapToInt(Integer::intValue).toArray();
        packetHandler.writeNoCache(entityUpdates);
        state.entityIds.clear();

        packetHandler.writeNoCache(new SetServerCamera(ClientCameraView.FirstPerson, true, null));

        packetHandler.tryFlush();

        state.stage.clearedWorld = true;
    }

    public boolean isPlaying(@Nonnull PlayerRef playerRef) {
        ReplayState state = states.get(playerRef.getUuid());
        return state != null;
    }

    public void start(@Nonnull PlayerRef playerRef, @Nonnull Path replayPath) {
        if (isPlaying(playerRef)) {
            clearWorld(playerRef, states.get(playerRef.getUuid()));
        }

        initState(playerRef.getUuid(), playerRef.getLanguage(), replayPath);

        PacketHandler handler = playerRef.getPacketHandler();
        ReplayState state = states.get(playerRef.getUuid());
        state.file.consumeConfigPhase((replayPacket) -> replayPacket.handle(handler, state));
        handler.tryFlush();

        state.stage.isFilteringPackets = true;
        state.stage.hasStarted = true;
    }

    public void initState(@Nonnull UUID uuid, @Nonnull String lang, @Nonnull Path replayPath) {
        ReplayState state = new ReplayState();
        state.replayPath = replayPath;
        state.playerUuid = uuid;
        state.lang = lang;
        // Run the first second so we don't see a blank world
        state.targetTick = 30;

        try {
            state.file = new ReplayInputFile(replayPath, protocol);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        states.put(uuid, state);

        try {
            state.loadTimelines();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void transfer(@Nonnull PlayerRef playerRef) {
        if (Constants.SINGLEPLAYER) {
            playerRef.getPacketHandler().disconnect(
                    Message.translation("replay.clickReconnectToAccessWorld")
            );
            return;
        }

        try {
            InetSocketAddress publicAddress = ServerManager.get().getLocalOrPublicAddress();
            assert publicAddress != null;
            playerRef.referToServer(publicAddress.getHostName(), publicAddress.getPort(), null);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }

    public void restart(@Nonnull ReplayState state) {
        if (!state.stage.hasStarted) {
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

        state.stage.clearedWorld = false;
        state.currentTick = 0;
    }

    public void stop(@Nonnull PlayerRef playerRef) {
        ReplayState state = states.get(playerRef.getUuid());
        if (state == null || !state.stage.hasStarted) {
            return;
        }

        stop(state);

        if (playerRef.isValid()) {
            transfer(playerRef);
        }
    }

    public void stop(@Nonnull ReplayState state) {
        states.remove(state.playerUuid);

        try {
            state.timeline.save(state.file.getMetadata().uuid, state.selectedTimeline);

            state.file.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.atInfo().log("Stopped replaying");
    }

    public void stopAll() {
        for (ReplayState state : states.values()) {
            stop(state);
        }
    }

    @Override
    public void tick(float v, int i, @Nonnull Store<EntityStore> store) {
        for (ReplayState state : states.values()) {
            tick(state);
        }
    }

    private void tick(@Nonnull ReplayState state) {
        if (!state.stage.hasStarted) {
            return;
        }

        PlayerRef playerRef = Universe.get().getPlayer(state.playerUuid);
        if (playerRef == null || !playerRef.isValid()) {
            return;
        }

        handlePage(state, playerRef);

        PacketHandler packetHandler = playerRef.getPacketHandler();
        try {
            state.stage.isProcessingPackets = true;

            if (!state.stage.clearedWorld) {
                clearWorld(playerRef, state);
                return;
            } else {
                int processedTicks = 0;
                while (canProcessPackets(state, packetHandler) &&
                       state.file.read((int) state.targetTick) &&
                       processedTicks < 500) { // TODO: investigate if necessary, and what value
                    ReplayPacket replayPacket = state.file.consumePacket();
                    replayPacket.handle(packetHandler, state);

                    if (replayPacket instanceof TickReplayPacket) {
                        tickEditor(state, playerRef, false);
                        processedTicks++;
                    }
                }
            }

            if ((!state.stage.sentJoinWorld || state.stage.isPlaying) && canProcessPackets(state, packetHandler)) {
                state.targetTick += Math.max(0.1, state.edit.speed);
            }

            tickEditor(state, playerRef, true);
        } catch (Exception e) {
            logger.atWarning().withCause(e).log("Error while processing replay packets");
        } finally {
            state.stage.isProcessingPackets = false;
        }
    }

    private void tickEditor(@Nonnull ReplayState state, @Nonnull PlayerRef playerRef, boolean move) {
        if (state.timeline.getLastSaved().plus(5, ChronoUnit.MINUTES).isBefore(Instant.now())) {
            state.timeline.save(state.file.getMetadata().uuid, state.selectedTimeline);
        }

        state.edit.speed = 1.0;

        Vector3d position = playerRef.getTransform().getPosition();
        Vector3f rotation = playerRef.getHeadRotation();
        state.edit.cameraPosition = new Position(position.x, position.y, position.z, rotation.x, rotation.y);

        if (state.stage.isPlaying && !state.ui.controlGame) {
            for (BaseProperty<?> property : state.timeline.getProperties().values()) {
                property.handle(state, state.currentTick);
            }

            if (move) {
                moveCamera(state, playerRef);
            }
        } else {
            playerRef.getPacketHandler().writeNoCache(new SetMovementStates(new SavedMovementStates(true)));
        }

        handleTimeDilatation(state, playerRef.getPacketHandler());
    }

    private void handleTimeDilatation(@Nonnull ReplayState state, @Nonnull PacketHandler packetHandler) {
        float speed = (float) state.edit.speed;
        if (state.ui.dragging || state.ui.controlGame || state.currentTick + speed < state.targetTick) {
            speed = 1;
        } else if (!state.stage.isPlaying) {
            speed = 0;
        }

        if (state.timeDilatation != speed) {
            state.timeDilatation = speed;
            packetHandler.writeNoCache(new SetTimeDilation(Math.min(Math.max(0.0101f, speed), 4)));
        }
    }

    public void moveCamera(@Nonnull ReplayState state, @Nonnull PlayerRef playerRef) {
        bypassFilter(state, () -> {
            Vector3d position = new Vector3d(
                    state.edit.cameraPosition.x(), state.edit.cameraPosition.y(), state.edit.cameraPosition.z()
            );
            Vector3f rotation = new Vector3f(
                    (float) state.edit.cameraPosition.yaw(), (float) state.edit.cameraPosition.pitch(), 0
            );

            Vector3f bodyRotation = new Vector3f(0.0F, rotation.getYaw(), 0.0F);

            ModelTransform transform = new ModelTransform(
                    PositionUtil.toPositionPacket(position),
                    PositionUtil.toDirectionPacket(bodyRotation),
                    PositionUtil.toDirectionPacket(rotation)
            );

            playerRef.getPacketHandler().writeNoCache(new SetMovementStates(new SavedMovementStates(true)));
            playerRef.getPacketHandler().writeNoCache(new ClientTeleport(
                    (byte) NEXT_TELEPORT_ID.getAndIncrement(),
                    transform,
                    false
            ));
        });
    }

    private void handlePage(@Nonnull ReplayState state, @Nonnull PlayerRef playerRef) {
        if (state.ui.controlGame) {
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

        CustomUIPage customPage = player.getPageManager().getCustomPage();
        if (customPage instanceof EditorUI ui) {
            editorUI = ui;
        } else if (customPage == null) {
            editorUI = new EditorUI(playerRef, this, state);
            player.getPageManager().openCustomPage(ref, store, editorUI);
        } else {
            return;
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
        if (!state.stage.sentJoinWorld) {
            return true;
        }

        CompletableFuture<Void> clientReadyForChunksFuture = packetHandler.getClientReadyForChunksFuture();
        return clientReadyForChunksFuture == null || clientReadyForChunksFuture.isDone();
    }

    public void bypassFilter(@Nonnull ReplayState state, @Nonnull Runnable runnable) {
        if (!state.stage.isFilteringPackets || state.stage.isProcessingPackets) {
            runnable.run();
            return;
        }

        state.stage.isProcessingPackets = true;
        try {
            runnable.run();
        } finally {
            state.stage.isProcessingPackets = false;
        }
    }

}
