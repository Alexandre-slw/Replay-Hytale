package gg.alexandre.replay.protocol;

import com.hypixel.hytale.server.core.io.PacketHandler;
import gg.alexandre.replay.replay.state.ReplayState;
import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;

public interface ReplayPacket {

    void deserialize(@Nonnull ByteBuf buffer);

    void serialize(@Nonnull ByteBuf buffer);

    void handle(@Nonnull PacketHandler packetHandler, @Nonnull ReplayState state);

}
