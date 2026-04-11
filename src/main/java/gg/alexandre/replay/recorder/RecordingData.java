package gg.alexandre.replay.recorder;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import gg.alexandre.replay.file.ReplayOutputFile;

import javax.annotation.Nonnull;
import java.time.Instant;

public class RecordingData {

    public final Instant start;
    public final ReplayOutputFile file;

    public int tick = 0;
    public PlayerRef watcher;

    public RecordingData(@Nonnull ReplayOutputFile file) {
        this.file = file;

        start = Instant.now();
    }

}
