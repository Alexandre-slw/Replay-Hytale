package gg.alexandre.replay.repository;

import com.google.gson.Gson;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.util.io.FileUtil;
import gg.alexandre.replay.ReplayPlugin;
import gg.alexandre.replay.cutscene.CutSceneMetadata;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class CutSceneRepository {

    public final static String CUTSCENE_EXTENSION = ".cutscene";

    public final Path cutScenesDirectory;

    public CutSceneRepository(@Nonnull Path dataDirectory) {
        cutScenesDirectory = dataDirectory.resolve("cutscenes");
    }

    public void saveCutScene(@Nonnull PlayerRef playerRef, @Nonnull String name, @Nonnull CutSceneMetadata metadata) {
        Path dir = cutScenesDirectory.resolve(playerRef.getUuid().toString());

        File directoryFile = dir.toFile();
        if (!directoryFile.exists()) {
            boolean created = directoryFile.mkdirs();
            if (!created) {
                throw new RuntimeException("Failed to create cutscenes directory");
            }
        }

        saveCutScene(dir.resolve(name + CUTSCENE_EXTENSION), metadata);
    }

    public void saveCutScene(@Nonnull Path path, @Nonnull CutSceneMetadata metadata) {
        Gson gson = ReplayPlugin.get().getGson();
        String json = gson.toJson(metadata);

        try {
            Files.createDirectories(path.getParent());
            FileUtil.writeStringAtomic(path, json);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Nonnull
    public List<Path> getCutScenes(@Nonnull PlayerRef playerRef) {
        File directory = cutScenesDirectory.resolve(playerRef.getUuid().toString()).toFile();
        if (!directory.exists()) {
            return List.of();
        }

        File[] values = directory.listFiles((_, name) -> name.endsWith(CUTSCENE_EXTENSION));
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

}
