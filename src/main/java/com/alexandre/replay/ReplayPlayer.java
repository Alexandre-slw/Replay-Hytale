package com.alexandre.replay;

import com.github.luben.zstd.Zstd;
import com.hypixel.hytale.component.Ref;
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
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;

import javax.annotation.Nonnull;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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
            return;
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

        if (length == 0) {
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
            PacketHandler packetHandler = player.getPacketHandler();
            Channel listener = packetHandler.getChannel();
            payload.resetReaderIndex();

            PacketRegistry.PacketInfo packetInfo = PacketRegistry.getToClientPacketById(packetId);
            if (packetInfo == null) {
                closeChannel(listener, payload);
                logger.atInfo().log("AAAAAA");
            } else if (length > packetInfo.maxSize()) {
                closeChannel(listener, payload);
                logger.atInfo().log("BBBBBB");
            } else {
                NetworkChannel channelVal = listener.attr(ProtocolUtil.STREAM_CHANNEL_KEY).get();
                if (channelVal != null && channelVal != packetInfo.channel()) {
                    closeChannel(listener, payload);
                    logger.atInfo().log("CCCCC");
                } else {
                    PacketStatsRecorder statsRecorder = listener.attr(PacketStatsRecorder.CHANNEL_KEY).get();
                    if (statsRecorder == null) {
                        statsRecorder = PacketStatsRecorder.NOOP;
                    }

                    try {
                        System.out.println(length + " " + packetInfo.name() + " " + packetInfo.id());

                        // TODO: compress on record
                        ByteBuf in = payload;
                        int inLength = length;
                        if (packetInfo.compressed()) {
                            ByteBuf compressed = Unpooled.buffer((int) Zstd.compressBound(length));
                            int compressedSize = compressToBuffer(in, compressed, 0, compressed.capacity());
                            in = compressed.slice(0, compressedSize);
                            inLength = compressedSize;
                        }

                        Packet p = PacketIO.readFramedPacketWithInfo(in, inLength, packetInfo, statsRecorder);
                        if (p instanceof Ping) {
                            continue;
                        }
                        // TODO: fixed by snapshots?
                        if (p instanceof RemoveAssets) {
                            continue;
                        }
                        packetHandler.writePacket((ToClientPacket) p, false);
                    } catch (ProtocolException | IndexOutOfBoundsException e) {
                        e.printStackTrace();
                        closeChannel(listener, payload);
                    }
                }
            }
        }
    }

    private static int compressToBuffer(@Nonnull ByteBuf src, @Nonnull ByteBuf dst, int dstOffset, int maxDstSize) {
        int COMPRESSION_LEVEL = Integer.getInteger("hytale.protocol.compressionLevel", Zstd.defaultCompressionLevel());
        if (src.isDirect() && dst.isDirect()) {
            return Zstd.compress(dst.nioBuffer(dstOffset, maxDstSize), src.nioBuffer(), COMPRESSION_LEVEL);
        } else {
            int srcSize = src.readableBytes();
            byte[] srcBytes = new byte[srcSize];
            src.getBytes(src.readerIndex(), srcBytes);
            byte[] compressed = Zstd.compress(srcBytes, COMPRESSION_LEVEL);
            dst.setBytes(dstOffset, compressed);
            return compressed.length;
        }
    }

    private void closeChannel(Channel listener, ByteBuf in) {
        in.skipBytes(in.readableBytes());
        ProtocolUtil.closeConnection(listener);
        logger.atInfo().log("Closed channel");
    }

}
