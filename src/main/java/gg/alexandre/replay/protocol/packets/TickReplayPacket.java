package gg.alexandre.replay.protocol.packets;

import com.hypixel.hytale.protocol.io.VarInt;
import com.hypixel.hytale.server.core.io.PacketHandler;
import gg.alexandre.replay.protocol.ReplayPacket;
import gg.alexandre.replay.replay.state.ReplayState;
import io.netty.buffer.ByteBuf;

public class TickReplayPacket implements ReplayPacket {

    private int tick;

    public TickReplayPacket() {
    }

    public TickReplayPacket(int tick) {
        this.tick = tick;
    }

    @Override
    public void deserialize(ByteBuf buffer) {
        tick = VarInt.read(buffer);
    }

    @Override
    public void serialize(ByteBuf buffer) {
        VarInt.write(buffer, tick);
    }

    @Override
    public void handle(PacketHandler packetHandler, ReplayState state) {

    }

    public int getTick() {
        return tick;
    }
}
