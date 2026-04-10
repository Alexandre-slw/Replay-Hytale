package gg.alexandre.replay.recorder;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import gg.alexandre.replay.file.ReplayOutputFile;

import java.time.Instant;

public class RecordingData {

    public final Instant start;
    public final ReplayOutputFile file;

    public int tick = 0;
    public PlayerRef watcher;

    public RecordingData(ReplayOutputFile file) {
        this.file = file;

        start = Instant.now();
    }

}
