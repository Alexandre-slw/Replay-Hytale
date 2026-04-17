package gg.alexandre.replay.protocol.packets;

import com.hypixel.hytale.protocol.io.VarInt;
import com.hypixel.hytale.server.core.io.PacketHandler;
import gg.alexandre.replay.protocol.ReplayPacket;
import gg.alexandre.replay.replay.state.ReplayState;
import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;

public class TickReplayPacket implements ReplayPacket {

    private int tick;

    public TickReplayPacket() {
    }

    public TickReplayPacket(int tick) {
        this.tick = tick;
    }

    @Override
    public void deserialize(@Nonnull ByteBuf buffer) {
        tick = VarInt.read(buffer);
    }

    @Override
    public void serialize(@Nonnull ByteBuf buffer) {
        VarInt.write(buffer, tick);
    }

    @Override
    public void handle(@Nonnull PacketHandler packetHandler, @Nonnull ReplayState state) {
        state.currentTick = tick;
    }

    public int getTick() {
        return tick;
    }
}
