package com.salwyrr.replay;

import com.github.luben.zstd.Zstd;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.tick.TickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.NetworkChannel;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.PacketRegistry;
import com.hypixel.hytale.protocol.ToClientPacket;
import com.hypixel.hytale.protocol.io.PacketIO;
import com.hypixel.hytale.protocol.io.PacketStatsRecorder;
import com.hypixel.hytale.protocol.io.ProtocolException;
import com.hypixel.hytale.protocol.io.netty.ProtocolUtil;
import com.hypixel.hytale.protocol.packets.connection.Ping;
import com.hypixel.hytale.protocol.packets.setup.RemoveAssets;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.io.ServerManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.salwyrr.protocol.packets.HytaleReplayPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;

import javax.annotation.Nonnull;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;

public class ReplayPlayer extends TickingSystem<EntityStore> {

    private boolean replaying = false;

    private String outputFile = "test.replay";
    private DataInputStream inputStream;

    private final HytaleLogger logger = HytaleLogger.forEnclosingClass();

    public void start() {
        stop();

        try {
            inputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(outputFile)));
        } catch (java.io.IOException e) {
            e.printStackTrace();
            return;
        }

        replaying = true;

        logger.atInfo().log("Started replaying");
    }

    public void stop() {
        if (!replaying) {
            return;
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

        // TODO: reset only to requesting user
        for (PlayerRef playerRef : Universe.get().getPlayers()) {
            World world = playerRef.getReference().getStore().getExternalData().getWorld();
            playerRef.removeFromStore();
            world.addPlayer(playerRef);
        }
    }

    @Override
    public void tick(float v, int i, @Nonnull Store<EntityStore> store) {
        if (!replaying || inputStream == null) {
            return;
        }

        try {
            for (int j = 0; j < 2 && inputStream.available() > 0; j++) {
                readPacket();
            }

            if (inputStream.available() == 0) {
                stop();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void readPacket() {
        ByteBuf in = Unpooled.buffer();
        int packetId;
        int length;

        try {
            packetId = inputStream.readInt();
            length = inputStream.readInt();
            byte[] data = new byte[length];
            inputStream.readFully(data);
            in.writeBytes(data);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        send(packetId, in, length);
        logger.atInfo().log("Sent packet");
    }

    public void send(int packetId, ByteBuf payload, int length) {
        System.out.println(ServerManager.get().getListeners().size());
        payload.markReaderIndex();
        // TODO: send only to requesting user
        for (PlayerRef player : Universe.get().getPlayers()) {
            payload.resetReaderIndex();

            // TODO
            new HytaleReplayPacket(payload).handle(player);
        }
    }

    private void closeChannel(Channel listener, ByteBuf in) {
        in.skipBytes(in.readableBytes());
        ProtocolUtil.closeConnection(listener);
        logger.atInfo().log("Closed channel");
    }

}
