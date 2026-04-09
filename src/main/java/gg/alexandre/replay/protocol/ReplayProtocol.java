package gg.alexandre.replay.protocol;

import gg.alexandre.replay.protocol.packets.EndSnapshotReplayPacket;
import gg.alexandre.replay.protocol.packets.HytaleReplayPacket;
import gg.alexandre.replay.protocol.packets.StartSnapshotReplayPacket;
import gg.alexandre.replay.protocol.packets.TickReplayPacket;

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
