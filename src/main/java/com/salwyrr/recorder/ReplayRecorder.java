package com.salwyrr.recorder;

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
import com.hypixel.hytale.protocol.packets.player.SetClientId;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.handlers.game.GamePacketHandler;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.salwyrr.protocol.ReplayPacket;
import com.salwyrr.protocol.ReplayProtocol;
import com.salwyrr.protocol.packets.HytaleReplayPacket;
import com.salwyrr.protocol.packets.TickReplayPacket;
import com.salwyrr.util.DummyUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import javax.annotation.Nonnull;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ReplayRecorder extends TickingSystem<EntityStore> {

    private boolean recording;

    // TODO: select file
    private String outputFile = "test.replay";
    private DataOutputStream outputStream;

    private final HytaleLogger logger = HytaleLogger.forEnclosingClass();

    private final ReplayProtocol protocol;

    private int writtenTick = -1;
    private int tick = 0;

    private Map<PlayerRef, Ref<EntityStore>> watchers = new HashMap<>();

    public ReplayRecorder(ReplayProtocol protocol) {
        this.protocol = protocol;

        registerPacketsListener();
    }

    public void start() {
        stop();

        try {
            outputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile)));
        } catch (java.io.IOException e) {
            e.printStackTrace();
            return;
        }

        writtenTick = -1;
        tick = 0;

        logger.atInfo().log("Started recording");

        recording = true;

        // TODO: reset only to requesting user
        for (PlayerRef player : Universe.get().getPlayers()) {
            Ref<EntityStore> ref = player.getReference();
            Store<EntityStore> store = ref.getStore();
            World world = store.getExternalData().getWorld();
            world.execute(() -> {
                DummyUtil.spawnDummyWatcher(player)
                        .thenAccept(watcher -> watchers.put(player, watcher));
            });
        }
    }

    public void stop() {
        if (!recording) {
            return;
        }

        recording = false;

        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        logger.atInfo().log("Stopped recording");

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
        PacketAdapters.registerOutbound((PacketHandler handler, Packet packet) -> {
            if (!recording) {
                return;
            }

            if (packet instanceof Ping) {
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
            boolean isDummy = ref.getLanguage().equals("dummy");

            if (!isDummy) {
                return;
            }

            if (packet instanceof SetClientId) {
                return;
            }

            write(toReplayPacket(packet));
        });
    }

    private ReplayPacket toReplayPacket(Packet packet) {
        ByteBuf buffer = Unpooled.buffer(packet.computeSize() + 256);

        Class<? extends Packet> type = packet.getClass();
        if (packet instanceof CachedPacket<?> cachedPacket) {
            type = cachedPacket.getPacketType();
        }

        PacketIO.writeFramedPacket(packet, type, buffer, PacketStatsRecorder.NOOP);

        return new HytaleReplayPacket(buffer);
    }

    private synchronized void write(ReplayPacket packet) {
        if (outputStream == null) {
            return;
        }

        if (writtenTick != tick) {
            writtenTick = tick;
            write(new TickReplayPacket(tick));
        }

        try {
            ByteBuf buffer = Unpooled.buffer();
            packet.serialize(buffer);

            outputStream.writeInt(protocol.getId(packet));
            outputStream.writeInt(buffer.readableBytes());
            buffer.readBytes(outputStream, buffer.readableBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
