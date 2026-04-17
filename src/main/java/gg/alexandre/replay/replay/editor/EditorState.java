package gg.alexandre.replay.replay.editor;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.hypixel.hytale.server.core.util.io.FileUtil;
import gg.alexandre.replay.ReplayPlugin;
import gg.alexandre.replay.replay.editor.properties.base.BaseProperty;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EditorState {

    private static final Path EDITS_DIRECTORY = ReplayPlugin.get().getDataDirectory().resolve("edits");

    @SerializedName("properties")
    private final List<BaseProperty<?>> properties = new ArrayList<>();

    @Nonnull
    public List<BaseProperty<?>> getProperties() {
        return properties;
    }

    public void save(UUID id) {
        Gson gson = ReplayPlugin.get().getGson();
        String json = gson.toJson(this);

        try {
            Files.createDirectories(EDITS_DIRECTORY);
            FileUtil.writeStringAtomic(EDITS_DIRECTORY.resolve(id + ".json"), json);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Nonnull
    public static EditorState load(UUID id) {
        Gson gson = ReplayPlugin.get().getGson();
        try {
            String json = Files.readString(EDITS_DIRECTORY.resolve(id + ".json"));
            return gson.fromJson(json, EditorState.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
