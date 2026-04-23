package gg.alexandre.replay.replay;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
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
import com.hypixel.hytale.protocol.packets.connection.Pong;
import com.hypixel.hytale.protocol.packets.entities.EntityUpdates;
import com.hypixel.hytale.protocol.packets.interaction.CancelInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.protocol.packets.interface_.*;
import com.hypixel.hytale.protocol.packets.player.*;
import com.hypixel.hytale.protocol.packets.setup.RequestAssets;
import com.hypixel.hytale.protocol.packets.setup.SetTimeDilation;
import com.hypixel.hytale.protocol.packets.setup.ViewRadius;
import com.hypixel.hytale.server.core.Constants;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.io.ServerManager;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PacketFilter;
import com.hypixel.hytale.server.core.io.handlers.SetupPacketHandler;
import com.hypixel.hytale.server.core.modules.entity.tracker.EntityTrackerSystems;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.modules.i18n.I18nModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.PositionUtil;
import gg.alexandre.replay.ReplayPlugin;
import gg.alexandre.replay.file.ReplayInputFile;
import gg.alexandre.replay.protocol.ReplayPacket;
import gg.alexandre.replay.protocol.ReplayProtocol;
import gg.alexandre.replay.protocol.packets.TickReplayPacket;
import gg.alexandre.replay.replay.editor.commands.SetKeyframeValueCommand;
import gg.alexandre.replay.replay.editor.properties.CameraProperty;
import gg.alexandre.replay.replay.editor.properties.base.BaseProperty;
import gg.alexandre.replay.replay.state.ReplayState;
import gg.alexandre.replay.ui.editor.EditorUI;
import gg.alexandre.replay.ui.manager.RealtimePageManager;
import gg.alexandre.replay.util.Position;
import gg.alexandre.replay.util.PositionTracker;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ReplayPlayer extends TickingSystem<EntityStore> {

    private static final AtomicInteger NEXT_TELEPORT_ID = new AtomicInteger();

    private final Path replayStatePath;

    private final ReplayProtocol protocol;
    private final Map<UUID, ReplayState> states = new ConcurrentHashMap<>();
    private final HytaleLogger logger = HytaleLogger.forEnclosingClass();

    public ReplayPlayer(@Nonnull ReplayProtocol protocol, @Nonnull Path dataDirectory) {
        this.protocol = protocol;
        replayStatePath = dataDirectory.resolve("state.json");

        PacketAdapters.registerInbound((PacketFilter) (handler, packet) -> {
            ReplayState state = getState(handler);
            if (state == null) {
                return false;
            }

            assert handler.getAuth() != null;
            if (state.playerUuid == null || !state.playerUuid.equals(handler.getAuth().getUuid())) {
                return false;
            }

            if (packet instanceof RequestAssets && !state.stage.isFilteringPackets) {
                state.file.consumeConfigPhase((replayPacket) -> replayPacket.handle(handler, state));
                handler.tryFlush();

                try {
                    Field receivedRequest = SetupPacketHandler.class.getDeclaredField("receivedRequest");
                    receivedRequest.setAccessible(true);
                    receivedRequest.set(handler, true);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                state.stage.isFilteringPackets = true;

                return true;
            }

            if (packet instanceof ClientReady clientReady) {
                state.stage.clientReady = true;

                if (clientReady.readyForChunks) {
                    state.stage.clearedWorld = true;
                    state.stage.hasStarted = true;
                }

                if (Constants.SINGLEPLAYER && clientReady.readyForGameplay && !state.stage.singleplayerHasRestarted) {
                    state.stage.singleplayerHasRestarted = true;
                    restart(state);
                }

                return false;
            }

            if (packet instanceof SyncInteractionChains syncInteractionChains) {
                handleInteractionChains(handler, state, syncInteractionChains);
                return true;
            }

            if (state.stage.isFilteringPackets && packet instanceof ClientDisconnect) {
                stop(state);
                return false;
            }

            if (packet instanceof ClientMovement movement) {
                PositionTracker.onClientMovement(state, movement);
            }

            if (state.stage.hasStarted) {
                return !(packet instanceof Pong || packet instanceof CustomPageEvent || packet instanceof ChatMessage);
            } else {
                return false;
            }
        });

        PacketAdapters.registerOutbound((PacketFilter) (handler, packet) -> {
            ReplayState state = getState(handler);
            if (state == null) {
                return false;
            }

            if (packet instanceof UpdateBlockHitboxes hitboxes) {
                assert hitboxes.blockBaseHitboxes != null;
                hitboxes.blockBaseHitboxes.replaceAll((_, _) -> new Hitbox[0]);
            }

            if (packet instanceof Ping ||
                    packet instanceof SetPage ||
                    packet instanceof CustomPage ||
                    packet instanceof ResetUserInterfaceState ||
                    packet instanceof UpdateAnchorUI ||
                    packet instanceof ViewRadius) {

                return packet instanceof CustomPage customPage &&
                        customPage.key != null && !customPage.key.startsWith("gg.alexandre.");
            }

            if (packet instanceof UpdateTranslations && !state.stage.sentTranslations) {
                state.stage.sentTranslations = true;
                I18nModule.get().sendTranslations(handler, state.lang);
                return true;
            }

            if (packet instanceof JoinWorld && !state.stage.sentJoinWorld) {
                state.stage.sentJoinWorld = true;
                return false;
            }

            boolean filter = state.stage.isFilteringPackets && !state.stage.isProcessingPackets;

            if (!filter) {
                if (packet instanceof EntityUpdates entityUpdates) {
                    if (entityUpdates.removed != null) {
                        for (int id : entityUpdates.removed) {
                            state.entityIds.remove(id);
                        }
                    }

                    if (entityUpdates.updates != null) {
                        for (EntityUpdate update : entityUpdates.updates) {
                            if (update.networkId == state.clientId && update.updates != null) {
                                for (ComponentUpdate data : update.updates) {
                                    if (data instanceof TransformUpdate transformUpdate) {
                                        Position pos;
                                        if (!state.position.sentInitialPosition &&
                                                state.file.getMetadata().position != null) {
                                            state.position.sentInitialPosition = true;
                                            pos = state.file.getMetadata().position;
                                        } else {
                                            pos = new Position(
                                                    state.position.x,
                                                    state.position.y,
                                                    state.position.z,
                                                    state.position.headPitch,
                                                    state.position.headYaw
                                            );
                                        }

                                        transformUpdate.transform.position = PositionUtil.toPositionPacket(
                                                new Vector3d(pos.x(), pos.y(), pos.z())
                                        );
                                        transformUpdate.transform.bodyOrientation = PositionUtil.toDirectionPacket(
                                                new Vector3f(0, (float) pos.pitch(), 0)
                                        );
                                        transformUpdate.transform.lookOrientation = PositionUtil.toDirectionPacket(
                                                new Vector3f((float) pos.yaw(), (float) pos.pitch(), 0)
                                        );

                                        PositionTracker.onTransformUpdate(state, transformUpdate);
                                    }
                                }
                            }

                            state.entityIds.add(update.networkId);
                        }
                    }
                }

                if (packet instanceof ClientTeleport teleport) {
                    PositionTracker.onClientTeleport(state, teleport);
                }
            }

            if (packet instanceof ShowEventTitle) {
                return true;
            }

            return filter;
        });
    }

    public void setup() {
        try {
            loadState();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleInteractionChains(@Nonnull PacketHandler handler, @Nonnull ReplayState state,
                                         @Nonnull SyncInteractionChains syncInteractionChains) {
        SyncInteractionChain interactionChain = null;

        for (int i = 0; i < syncInteractionChains.updates.length; i++) {
            SyncInteractionChain chain = syncInteractionChains.updates[i];
            if (chain.interactionType == InteractionType.Primary && chain.initial) {
                interactionChain = chain;
                break;
            }
        }

        if (interactionChain == null) {
            return;
        }

        SyncInteractionChain chain = interactionChain;
        bypassFilter(state, () ->
                handler.writeNoCache(new CancelInteractionChain(chain.chainId, chain.forkedId))
        );
        state.ui.controlGame = false;

        if (state.ui.editingCamera && state.ui.selectedKeyframe != null) {
            state.commandsStack.execute(new SetKeyframeValueCommand(
                    state,
                    state.ui.selectedKeyframe.propertyId(),
                    state.ui.selectedKeyframe.tick(),
                    new Position(
                            state.position.x,
                            state.position.y,
                            state.position.z,
                            state.position.headPitch,
                            state.position.headYaw
                    )
            ));
        }

        state.ui.editingCamera = false;
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

        EntityTrackerSystems.EntityViewer viewer = store.getComponent(
                ref, EntityTrackerSystems.EntityViewer.getComponentType()
        );
        if (viewer != null) {
            state.entityIds.addAll(viewer.visible.stream().map(entityStoreRef -> {
                NetworkId networkId = store.getComponent(entityStoreRef, NetworkId.getComponentType());
                assert networkId != null;
                return networkId.getId();
            }).toList());
        }

        PacketHandler packetHandler = playerRef.getPacketHandler();

        state.entityIds.remove(state.clientId);

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
        transfer(playerRef, true);
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

    private void loadState() throws IOException {
        if (!Files.exists(replayStatePath)) {
            return;
        }

        try (JsonReader reader = new JsonReader(new FileReader(replayStatePath.toFile()))) {
            JsonObject json = ReplayPlugin.get().getGson().fromJson(reader, JsonObject.class);

            long timestamp = json.get("timestamp").getAsLong();
            if (Instant.ofEpochMilli(timestamp).plus(5, ChronoUnit.MINUTES).isBefore(Instant.now())) {
                logger.atWarning().log("Expired replay state, ignoring");
                return;
            }

            UUID uuid = UUID.fromString(json.get("uuid").getAsString());
            String lang = json.get("lang").getAsString();
            Path replayPath = Path.of(json.get("replayPath").getAsString());

            initState(uuid, lang, replayPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            Files.delete(replayStatePath);
        }
    }

    private void saveState(@Nonnull ReplayState state) {
        JsonObject json = new JsonObject();
        json.addProperty("uuid", state.playerUuid.toString());
        json.addProperty("lang", state.lang);
        json.addProperty("replayPath", state.replayPath.toString());
        json.addProperty("timestamp", System.currentTimeMillis());

        try (JsonWriter writer = new JsonWriter(new FileWriter(replayStatePath.toFile()))) {
            ReplayPlugin.get().getGson().toJson(json, writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void transfer(@Nonnull PlayerRef playerRef, boolean replay) {
        if (Constants.SINGLEPLAYER) {
            if (replay) {
                saveState(states.get(playerRef.getUuid()));
            }

            playerRef.getPacketHandler().disconnect(
                    replay ?
                            Message.translation("replay.clickReconnectToAccessReplay") :
                            Message.translation("replay.clickReconnectToAccessWorld")
            );
            return;
        }

        try {
            byte[] referralData = null;
            if (replay) {
                // Some plugins might want to know that this is a replay
                JsonObject referral = new JsonObject();
                referral.addProperty("replay", true);

                referralData = ReplayPlugin.get().getGson().toJson(referral).getBytes(StandardCharsets.UTF_8);
            }

            InetSocketAddress publicAddress = ServerManager.get().getLocalOrPublicAddress();
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
            transfer(playerRef, false);
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
                int processedPackets = 0;
                while (canProcessPackets(state) && state.file.read((int) state.targetTick) && processedPackets < 400) {
                    ReplayPacket replayPacket = state.file.consumePacket();
                    replayPacket.handle(packetHandler, state);
                    processedPackets++;

                    if (replayPacket instanceof TickReplayPacket) {
                        tickEditor(state, playerRef, false);
                    }
                }
            }

            if ((!state.stage.sentJoinWorld || state.stage.isPlaying) && canProcessPackets(state)) {
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

        state.edit.cameraPosition = new Position(
                state.position.x,
                state.position.y,
                state.position.z,
                state.position.headPitch, // yaw and pitch from packets is inverted?
                state.position.headYaw
        );

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

        handleTimeDilation(state, playerRef.getPacketHandler(), move);

        if (move && state.timeline.getProperties().containsKey("camera")) {
            handleCameraPathDisplay(state, playerRef);
        }
    }

    private void handleCameraPathDisplay(@Nonnull ReplayState state, @Nonnull PlayerRef playerRef) {
        if (state.stage.isPlaying) {
            state.overlay.clearImmediately(playerRef);
            return;
        }

        if (!state.overlay.shouldRender()) {
            return;
        }

        CameraProperty cameraProperty = (CameraProperty) state.timeline.getProperties().get("camera");

        TreeMap<Integer, Position> values = cameraProperty.getValues();
        if (values.isEmpty()) {
            return;
        }

        List<Position> positions = values.values().stream().toList();

        List<Vector3d> lines = new ArrayList<>();
        int ticksResolution = 10;
        int maxTicksWindow = 30 * 60;

        int from = Math.max(values.firstKey(), state.currentTick - maxTicksWindow);
        int to = Math.min(values.lastKey(), state.currentTick + maxTicksWindow);

        for (int i = from; i <= to; i += ticksResolution) {
            Position position = cameraProperty.getValue(i);
            if (position == null) {
                continue;
            }

            lines.add(new Vector3d(position.x(), position.y(), position.z()));
        }

        Vector3d playerPosition = new Vector3d(state.position.x, state.position.y, state.position.z);

        state.overlay.renderTo(playerRef, positions, lines, playerPosition, cameraProperty.getValue(state.currentTick));
    }

    private void handleTimeDilation(@Nonnull ReplayState state, @Nonnull PacketHandler packetHandler, boolean move) {
        float speed = (float) state.edit.speed;
        if (state.ui.dragging || state.ui.controlGame || state.currentTick + speed <= state.targetTick) {
            state.overrideTimeDilation = true;
            speed = 1;
        } else if (state.overrideTimeDilation) {
            state.overrideTimeDilation = !move;
            speed = 1;
        } else if (!state.stage.isPlaying) {
            speed = 0;
        }

        if (state.timeDilation != speed) {
            state.timeDilation = speed;
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

    private boolean canProcessPackets(@Nonnull ReplayState state) {
        if (!state.stage.sentJoinWorld) {
            return true;
        }

        return state.stage.clientReady;
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
