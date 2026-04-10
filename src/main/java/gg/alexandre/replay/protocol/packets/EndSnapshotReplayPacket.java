package gg.alexandre.replay.protocol.packets;

import com.hypixel.hytale.server.core.io.PacketHandler;
import gg.alexandre.replay.protocol.ReplayPacket;
import gg.alexandre.replay.replay.state.ReplayState;
import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;

public class EndSnapshotReplayPacket implements ReplayPacket {

    public EndSnapshotReplayPacket() {
    }

    @Override
    public void deserialize(@Nonnull ByteBuf buffer) {

    }

    @Override
    public void serialize(@Nonnull ByteBuf buffer) {

    }

    @Override
    public void handle(@Nonnull PacketHandler packetHandler, @Nonnull ReplayState state) {

    }

}
