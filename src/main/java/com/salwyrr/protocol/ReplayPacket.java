package com.salwyrr.protocol;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import io.netty.buffer.ByteBuf;

public interface ReplayPacket {

    void deserialize(ByteBuf buffer);

    void serialize(ByteBuf buffer);

    void handle(PlayerRef playerRef);

}
