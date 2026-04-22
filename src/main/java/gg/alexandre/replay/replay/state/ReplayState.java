package gg.alexandre.replay.replay.state;

import com.google.gson.Gson;
import gg.alexandre.replay.ReplayPlugin;
import gg.alexandre.replay.file.ReplayInputFile;
import gg.alexandre.replay.replay.editor.properties.CameraProperty;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class ReplayState {

    public Path replayPath;
    public ReplayInputFile file;

    public int currentTick;
    public double targetTick;

    public UUID playerUuid;
    public String lang;

    public double timeDilatation;

    public Set<Integer> entityIds = new HashSet<>();
    public int clientId;

    public EditState edit = new EditState();
    public ReplayStageState stage = new ReplayStageState();
    public UIState ui = new UIState();
    public TimelineState timeline = new TimelineState();

    public String selectedTimeline;
    public List<String> timelines = new ArrayList<>();

    public void loadTimelines() throws IOException {
        Path dir = TimelineState.EDITS_DIRECTORY.resolve(file.getMetadata().uuid.toString());
        List<String> timelines;

        if (Files.isDirectory(dir)) {
            try (Stream<Path> stream = Files.list(dir)) {
                timelines = stream
                        .map(path -> path.getFileName().toString())
                        .filter(path -> path.toLowerCase(Locale.ROOT).endsWith(".json"))
                        .map(path -> path.substring(0, path.length() - 5))
                        .toList();
            }
        } else {
            timelines = List.of(TimelineState.DEFAULT_TIMELINE_NAME);
        }

        this.timelines.addAll(timelines);

        if (this.timelines.isEmpty() || this.timelines.contains(TimelineState.DEFAULT_TIMELINE_NAME)) {
            loadTimeline(TimelineState.DEFAULT_TIMELINE_NAME);
        } else {
            loadTimeline(this.timelines.getFirst());
        }
    }

    public void loadTimeline(@Nonnull String name) {
        if (selectedTimeline != null) {
            timeline.save(file.getMetadata().uuid, selectedTimeline);
        }

        Path path = TimelineState.getTimelinePath(file.getMetadata().uuid, name);

        if (!Files.exists(path)) {
            timeline = new TimelineState();
            timeline.getProperties().put("camera", new CameraProperty());
            timeline.save(file.getMetadata().uuid, name);
        } else {
            Gson gson = ReplayPlugin.get().getGson();
            try {
                String json = Files.readString(path);
                timeline = gson.fromJson(json, TimelineState.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        selectedTimeline = name;
        if (!timelines.contains(name)) {
            timelines.add(name);
        }
    }

}
