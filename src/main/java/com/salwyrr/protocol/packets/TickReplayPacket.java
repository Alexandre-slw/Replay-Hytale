package com.salwyrr.protocol.packets;

import com.hypixel.hytale.protocol.io.VarInt;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.salwyrr.protocol.ReplayPacket;
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
    public void handle(PlayerRef playerRef) {

    }

}
