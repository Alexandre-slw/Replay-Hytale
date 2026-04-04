package com.salwyrr.repository;

import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.io.File;
import java.nio.file.Path;
import java.text.DateFormat;
import java.util.List;
import java.util.stream.Stream;

public class ReplayRepository {

    public final static Path REPLAY_DIRECTORY = Path.of("replays");
    public final static String REPLAY_EXTENSION = ".replay";

    public Path newReplay(PlayerRef playerRef) {
        Path dir = REPLAY_DIRECTORY.resolve(playerRef.getUuid().toString());

        File directoryFile = dir.toFile();
        if (!directoryFile.exists()) {
            boolean created = directoryFile.mkdirs();
            if (!created) {
                throw new RuntimeException("Failed to create replay directory");
            }
        }

        String name = DateFormat.getDateTimeInstance().format(System.currentTimeMillis());
        return dir.resolve(name + REPLAY_EXTENSION);
    }

    public List<Path> getReplays(PlayerRef playerRef) {
        File directory = REPLAY_DIRECTORY.resolve(playerRef.getUuid().toString()).toFile();
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

}
