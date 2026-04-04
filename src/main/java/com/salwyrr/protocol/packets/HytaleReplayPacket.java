package com.salwyrr.protocol.packets;

import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.PacketRegistry;
import com.hypixel.hytale.protocol.ToClientPacket;
import com.hypixel.hytale.protocol.io.PacketIO;
import com.hypixel.hytale.protocol.io.PacketStatsRecorder;
import com.hypixel.hytale.protocol.io.ProtocolException;
import com.hypixel.hytale.protocol.packets.setup.RemoveAssets;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.salwyrr.protocol.ReplayPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class HytaleReplayPacket implements ReplayPacket {

    private ByteBuf data;

    public HytaleReplayPacket() {
    }

    public HytaleReplayPacket(ByteBuf data) {
        this.data = data;
    }

    @Override
    public void deserialize(ByteBuf buffer) {
        data = buffer;
    }

    @Override
    public void serialize(ByteBuf buffer) {
        buffer.writeBytes(data);
    }

    @Override
    public void handle(PlayerRef playerRef) {
        PacketHandler packetHandler = playerRef.getPacketHandler();

        int packetId = data.readIntLE();
        PacketRegistry.PacketInfo info = PacketRegistry.getToClientPacketById(packetId);
        if (info == null) {
            throw new ProtocolException("Unknown packet ID: " + packetId);
        }

        Packet p = PacketIO.readFramedPacketWithInfo(data, data.readableBytes(), info, PacketStatsRecorder.NOOP);
        // TODO: fixed by snapshots?
        if (p instanceof RemoveAssets) {
            return;
        }

        packetHandler.writePacket((ToClientPacket) p, false);
    }

}
