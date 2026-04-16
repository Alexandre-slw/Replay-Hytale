package gg.alexandre.replay.protocol.packets;

import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.PacketRegistry;
import com.hypixel.hytale.protocol.ToClientPacket;
import com.hypixel.hytale.protocol.io.PacketIO;
import com.hypixel.hytale.protocol.io.PacketStatsRecorder;
import com.hypixel.hytale.protocol.io.ProtocolException;
import com.hypixel.hytale.protocol.packets.interface_.*;
import com.hypixel.hytale.protocol.packets.player.JoinWorld;
import com.hypixel.hytale.server.core.io.PacketHandler;
import gg.alexandre.replay.protocol.ReplayPacket;
import gg.alexandre.replay.replay.state.ReplayState;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import javax.annotation.Nonnull;

public class HytaleReplayPacket implements ReplayPacket {

    private ByteBuf data;

    public HytaleReplayPacket() {
    }

    public HytaleReplayPacket(@Nonnull ByteBuf data) {
        this.data = data;
    }

    @Override
    public void deserialize(@Nonnull ByteBuf buffer) {
        data = Unpooled.buffer(buffer.readableBytes());
        data.writeBytes(buffer, buffer.readerIndex(), buffer.readableBytes());
    }

    @Override
    public void serialize(@Nonnull ByteBuf buffer) {
        buffer.writeBytes(data, data.readerIndex(), data.readableBytes());
    }

    @Override
    public void handle(@Nonnull PacketHandler packetHandler, @Nonnull ReplayState state) {
        int length = data.readIntLE();
        int packetId = data.readIntLE();
        PacketRegistry.PacketInfo info = PacketRegistry.getToClientPacketById(packetId);
        if (info == null) {
            throw new ProtocolException("Unknown packet ID: " + packetId);
        }

        Packet packet = PacketIO.readFramedPacketWithInfo(data, length, info, PacketStatsRecorder.NOOP);

        if (packet instanceof JoinWorld) {
            if (state.sentJoinWorld) {
                return;
            }

            state.sentJoinWorld = true;
        }

        packetHandler.write((ToClientPacket) packet);
    }

}
