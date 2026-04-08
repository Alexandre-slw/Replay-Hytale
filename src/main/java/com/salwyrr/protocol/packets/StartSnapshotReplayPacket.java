package com.salwyrr.protocol.packets;

import com.hypixel.hytale.server.core.io.PacketHandler;
import com.salwyrr.protocol.ReplayPacket;
import com.salwyrr.replay.state.ReplayState;
import io.netty.buffer.ByteBuf;

public class StartSnapshotReplayPacket implements ReplayPacket {

    public StartSnapshotReplayPacket() {
    }

    @Override
    public void deserialize(ByteBuf buffer) {

    }

    @Override
    public void serialize(ByteBuf buffer) {

    }

    @Override
    public void handle(PacketHandler packetHandler, ReplayState state) {

    }

}
