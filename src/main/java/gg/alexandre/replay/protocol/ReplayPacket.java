package gg.alexandre.replay.protocol;

import com.hypixel.hytale.server.core.io.PacketHandler;
import gg.alexandre.replay.replay.state.ReplayState;
import io.netty.buffer.ByteBuf;

public interface ReplayPacket {

    void deserialize(ByteBuf buffer);

    void serialize(ByteBuf buffer);

    void handle(PacketHandler packetHandler, ReplayState state);

}
