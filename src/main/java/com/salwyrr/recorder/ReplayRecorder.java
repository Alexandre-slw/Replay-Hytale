package com.salwyrr.recorder;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.tick.TickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.CachedPacket;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.PacketRegistry;
import com.hypixel.hytale.protocol.io.PacketIO;
import com.hypixel.hytale.protocol.io.PacketStatsRecorder;
import com.hypixel.hytale.protocol.packets.connection.Ping;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.salwyrr.protocol.ReplayPacket;
import com.salwyrr.protocol.ReplayProtocol;
import com.salwyrr.protocol.packets.HytaleReplayPacket;
import com.salwyrr.protocol.packets.TickReplayPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import javax.annotation.Nonnull;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class ReplayRecorder extends TickingSystem<EntityStore> {

    private boolean recording;

    // TODO: select file
    private String outputFile = "test.replay";
    private DataOutputStream outputStream;

    private final HytaleLogger logger = HytaleLogger.forEnclosingClass();

    private final ReplayProtocol protocol;

    private int writtenTick = -1;
    private int tick = 0;

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
        for (PlayerRef playerRef : Universe.get().getPlayers()) {
            World world = playerRef.getReference().getStore().getExternalData().getWorld();
            world.execute(() -> {
                playerRef.removeFromStore();
                world.addPlayer(playerRef);
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
