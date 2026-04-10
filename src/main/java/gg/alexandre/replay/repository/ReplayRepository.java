package gg.alexandre.replay.repository;

import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.io.File;
import java.nio.file.Path;
import java.text.DateFormat;
import java.util.List;
import java.util.stream.Stream;

public class ReplayRepository {

    public final static String REPLAY_EXTENSION = ".replay";

    public final Path replayDirectory;

    public ReplayRepository(Path dataDirectory) {
        replayDirectory = dataDirectory.resolve("replays");
    }

    public Path newReplay(PlayerRef playerRef) {
        Path dir = replayDirectory.resolve(playerRef.getUuid().toString());

        File directoryFile = dir.toFile();
        if (!directoryFile.exists()) {
            boolean created = directoryFile.mkdirs();
            if (!created) {
                throw new RuntimeException("Failed to create replay directory");
            }
        }

        String name = DateFormat.getDateTimeInstance().format(System.currentTimeMillis())
                .replace(":", "-")
                .replace("/", "-")
                .replace("\u202F", " ");
        return dir.resolve(name + REPLAY_EXTENSION);
    }

    public List<Path> getReplays(PlayerRef playerRef) {
        File directory = replayDirectory.resolve(playerRef.getUuid().toString()).toFile();
        if (!directory.exists()) {
            return List.of();
        }

        File[] values = directory.listFiles((_, name) -> name.endsWith(REPLAY_EXTENSION));
        if (values == null) {
            return List.of();
        }

        return Stream.of(
                values
        ).map(File::toPath).toList();
    }

    public Path getReplay(PlayerRef playerRef, String name) {
        if (!name.endsWith(REPLAY_EXTENSION)) {
            name += REPLAY_EXTENSION;
        }

        return replayDirectory.resolve(playerRef.getUuid().toString()).resolve(name);
    }
}
