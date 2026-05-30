package gg.alexandre.replay.recorder;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import gg.alexandre.replay.file.ReplayOutputFile;
import gg.alexandre.replay.util.Position;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class RecordingData {

    public final Instant start;
    public final ReplayOutputFile file;

    public Position position;
    public int tick = 0;
    public PlayerRef watcher;

    public Instant lastSnapshot = Instant.now();
    public Map<Integer, Integer> snapshotOffsets = new HashMap<>();

    public RecordingData(@Nonnull ReplayOutputFile file) {
        this.file = file;

        start = Instant.now();
    }

}
