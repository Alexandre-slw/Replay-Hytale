package com.salwyrr.protocol;

import com.salwyrr.protocol.packets.EndSnapshotReplayPacket;
import com.salwyrr.protocol.packets.HytaleReplayPacket;
import com.salwyrr.protocol.packets.StartSnapshotReplayPacket;
import com.salwyrr.protocol.packets.TickReplayPacket;

import java.util.List;

public class ReplayProtocol {

    private final List<Class<? extends ReplayPacket>> packets;

    public ReplayProtocol() {
        packets = List.of(
                TickReplayPacket.class,
                HytaleReplayPacket.class,
                StartSnapshotReplayPacket.class,
                EndSnapshotReplayPacket.class
        );
    }

    public int getId(ReplayPacket packet) {
        return packets.indexOf(packet.getClass());
    }

    public ReplayPacket getInstance(int id) {
        try {
            return packets.get(id).getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
