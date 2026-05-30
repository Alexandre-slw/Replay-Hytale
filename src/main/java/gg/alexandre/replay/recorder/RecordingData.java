package gg.alexandre.replay.recorder;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import gg.alexandre.replay.file.ReplayOutputFile;
import gg.alexandre.replay.util.Position;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.TreeMap;

public class RecordingData {

    public final Instant start;
    public final ReplayOutputFile file;

    public Position position;
    public int tick = 0;
    public PlayerRef watcher;

    public int lastSnapshotTick = 0;
    public TreeMap<Integer, Integer> snapshotOffsets = new TreeMap<>();

    public RecordingData(@Nonnull ReplayOutputFile file) {
        this.file = file;

        start = Instant.now();
    }

}
