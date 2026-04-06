package com.salwyrr.protocol.packets;

import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.PacketRegistry;
import com.hypixel.hytale.protocol.ToClientPacket;
import com.hypixel.hytale.protocol.io.PacketIO;
import com.hypixel.hytale.protocol.io.PacketStatsRecorder;
import com.hypixel.hytale.protocol.io.ProtocolException;
import com.hypixel.hytale.protocol.packets.player.JoinWorld;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.salwyrr.protocol.ReplayPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.List;

public class HytaleReplayPacket implements ReplayPacket {

    private ByteBuf data;

    public HytaleReplayPacket() {
    }

    public HytaleReplayPacket(ByteBuf data) {
        this.data = data;
    }

    @Override
    public void deserialize(ByteBuf buffer) {
        data = Unpooled.buffer(buffer.readableBytes());
        data.writeBytes(buffer, buffer.readerIndex(), buffer.readableBytes());
    }

    @Override
    public void serialize(ByteBuf buffer) {
        buffer.writeBytes(data, data.readerIndex(), data.readableBytes());
    }

    @Override
    public void handle(PlayerRef playerRef) {
        PacketHandler packetHandler = playerRef.getPacketHandler();

        int length = data.readIntLE();
        int packetId = data.readIntLE();
        PacketRegistry.PacketInfo info = PacketRegistry.getToClientPacketById(packetId);
        if (info == null) {
            throw new ProtocolException("Unknown packet ID: " + packetId);
        }

        Packet p = PacketIO.readFramedPacketWithInfo(data, length, info, PacketStatsRecorder.NOOP);
        if (p instanceof JoinWorld packet) {
            packet.fadeInOut = false;
        }

        packetHandler.write((ToClientPacket) p);

        if (p instanceof JoinWorld) {
            packetHandler.tryFlush();
            packetHandler.setQueuePackets(true);
        }
    }

}
