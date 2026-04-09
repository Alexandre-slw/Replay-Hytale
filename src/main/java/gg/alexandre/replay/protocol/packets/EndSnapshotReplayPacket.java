package gg.alexandre.replay.protocol.packets;

import com.hypixel.hytale.server.core.io.PacketHandler;
import gg.alexandre.replay.protocol.ReplayPacket;
import gg.alexandre.replay.replay.state.ReplayState;
import io.netty.buffer.ByteBuf;

public class EndSnapshotReplayPacket implements ReplayPacket {

    public EndSnapshotReplayPacket() {
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
