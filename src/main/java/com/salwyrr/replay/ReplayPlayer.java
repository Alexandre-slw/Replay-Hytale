package com.salwyrr.replay;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.tick.TickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.packets.connection.Ping;
import com.hypixel.hytale.protocol.packets.entities.EntityUpdates;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PacketFilter;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.salwyrr.protocol.ReplayPacket;
import com.salwyrr.protocol.ReplayProtocol;
import com.salwyrr.protocol.packets.TickReplayPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import javax.annotation.Nonnull;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class ReplayPlayer extends TickingSystem<EntityStore> {

    private boolean replaying = false;

    private String outputFile = "test.replay";
    private DataInputStream inputStream;

    private final HytaleLogger logger = HytaleLogger.forEnclosingClass();

    private final ReplayProtocol protocol;

    private int tick = 0;
    private ReplayPacket packet;
    private boolean processingPackets;

    public ReplayPlayer(ReplayProtocol protocol) {
        this.protocol = protocol;

        PacketAdapters.registerOutbound((PacketFilter) (_, packet) -> !(packet instanceof Ping) && replaying && !processingPackets);
    }

    public void start() {
        try {
            inputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(outputFile)));
        } catch (java.io.IOException e) {
            e.printStackTrace();
            return;
        }

        tick = 0;
        packet = null;

        // TODO: setup only for requesting user
        for (PlayerRef playerRef : Universe.get().getPlayers()) {
            Ref<EntityStore> reference = playerRef.getReference();
            Store<EntityStore> store = reference.getStore();

            store.getExternalData().getWorld().execute(() -> {
                NetworkId networkId = store.getComponent(reference, NetworkId.getComponentType());
                assert networkId != null;

                EntityUpdates entityUpdates = new EntityUpdates();
                entityUpdates.removed = new int[] { networkId.getId() };

                playerRef.getPacketHandler().write(entityUpdates);

                replaying = true;

                logger.atInfo().log("Started replaying");
            });
        }
    }

    public CompletableFuture<Void> stop() {
        if (!replaying) {
            return CompletableFuture.completedFuture(null);
        }

        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
        }

        replaying = false;

        logger.atInfo().log("Stopped replaying");

        CompletableFuture<Void> future = new CompletableFuture<>();

        // TODO: reset only to requesting user
        for (PlayerRef playerRef : Universe.get().getPlayers()) {
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

        return future;
    }

    @Override
    public void tick(float v, int i, @Nonnull Store<EntityStore> store) {
        // TODO: verify player is still here
        if (!replaying || inputStream == null || Universe.get().getPlayers().isEmpty()) {
            return;
        }

        try {
            processingPackets = true;
            while (inputStream.available() > 0) {
                if (!processPacket()) {
                    break;
                }
            }

            if (inputStream.available() == 0) {
                stop();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            processingPackets = false;
        }

        tick++;
    }

    private boolean processPacket() {
        if (packet == null) {
            packet = readPacket();
        }

        if (packet instanceof TickReplayPacket tickReplayPacket) {
            if (tickReplayPacket.getTick() > tick) {
                return false;
            }
        } else {
            // TODO: send only to requesting user
            for (PlayerRef player : Universe.get().getPlayers()) {
                packet.handle(player);
            }
        }

        packet = null;

        return true;
    }

    private ReplayPacket readPacket() {
        ByteBuf in;
        int packetId;
        int length;

        try {
            packetId = inputStream.readInt();
            length = inputStream.readInt();
            byte[] data = new byte[length];
            inputStream.readFully(data);
            in = Unpooled.buffer(length);
            in.writeBytes(data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ReplayPacket packet = protocol.getInstance(packetId);
        packet.deserialize(in);
        return packet;
    }

}
