package gg.alexandre.replay.replay.state;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.hypixel.hytale.server.core.util.io.FileUtil;
import gg.alexandre.replay.ReplayPlugin;
import gg.alexandre.replay.replay.editor.properties.base.BaseProperty;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TimelineState {

    public static final Path EDITS_DIRECTORY = ReplayPlugin.get().getDataDirectory().resolve("edits");
    public static final String DEFAULT_TIMELINE_NAME = "Default";

    @SerializedName("properties")
    private final Map<String, BaseProperty<?>> properties = new HashMap<>();

    private Instant lastSaved = Instant.now();

    public void save(@Nonnull UUID id, @Nonnull String name) {
        Gson gson = ReplayPlugin.get().getGson();
        String json = gson.toJson(this);

        try {
            Path path = getTimelinePath(id, name);

            Files.createDirectories(path.getParent());
            FileUtil.writeStringAtomic(path, json);

            lastSaved = Instant.now();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Path getTimelinePath(@Nonnull UUID id, @Nonnull String name) {
        return EDITS_DIRECTORY.resolve(id.toString()).resolve(name + ".json");
    }

    @Nonnull
    public Map<String, BaseProperty<?>> getProperties() {
        return properties;
    }

    @Nonnull
    public Instant getLastSaved() {
        return lastSaved;
    }
}
