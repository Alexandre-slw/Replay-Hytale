package gg.alexandre.replay.repository;

import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import java.io.File;
import java.nio.file.Path;
import java.text.DateFormat;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class ReplayRepository {

    public final static String REPLAY_EXTENSION = ".replay";

    public final Path replayDirectory;

    public ReplayRepository(@Nonnull Path dataDirectory) {
        replayDirectory = dataDirectory.resolve("replays");
    }

    @Nonnull
    public Path newReplay(@Nonnull PlayerRef playerRef) {
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

    @Nonnull
    public List<Path> getReplays(@Nonnull PlayerRef playerRef) {
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
                )
                .sorted(Comparator.comparingLong(File::lastModified).reversed())
                .map(File::toPath)
                .toList();
    }

    @Nonnull
    public Path getReplay(@Nonnull PlayerRef playerRef, @Nonnull String name) {
        if (!name.endsWith(REPLAY_EXTENSION)) {
            name += REPLAY_EXTENSION;
        }

        return replayDirectory.resolve(playerRef.getUuid().toString()).resolve(name);
    }
}
