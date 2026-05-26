package gg.alexandre.replay.replay.state;

import com.google.gson.Gson;
import gg.alexandre.replay.ReplayPlugin;
import gg.alexandre.replay.cutscene.CutSceneMetadata;
import gg.alexandre.replay.file.ReplayInputFile;
import gg.alexandre.replay.replay.CameraManager;
import gg.alexandre.replay.replay.editor.commands.CommandsStack;
import gg.alexandre.replay.replay.editor.properties.CameraProperty;
import gg.alexandre.replay.util.CameraPathDebugOverlay;
import gg.alexandre.replay.util.FovPacketUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class ReplayState {

    public Path path;
    @Nullable
    public ReplayInputFile file;

    public int currentTick;
    public double targetTick;

    public UUID playerUuid;
    public String lang;

    public double timeDilation;
    public boolean overrideTimeDilation;

    public CutSceneMetadata cutSceneMetadata;
    public boolean useEditor = true;

    public Set<Integer> entityIds = new HashSet<>();
    public int clientId;

    public int clientViewRadius = 16 * 32;

    public FovPacketUtil fovUtil;

    public EditState edit = new EditState();
    public ReplayStageState stage = new ReplayStageState();
    public UIState ui = new UIState();
    public TimelineState timeline = new TimelineState();
    public PositionState position = new PositionState();

    public CameraPathDebugOverlay overlay = new CameraPathDebugOverlay();
    public CameraManager cameraManager = new CameraManager();

    public CommandsStack commandsStack = new CommandsStack();

    public String selectedTimeline;
    public List<String> timelines = new ArrayList<>();

    public void loadTimelines(UUID uuid) throws IOException {
        Path dir = TimelineState.EDITS_DIRECTORY.resolve(uuid.toString());
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
            loadTimeline(uuid, TimelineState.DEFAULT_TIMELINE_NAME);
        } else {
            loadTimeline(uuid, this.timelines.getFirst());
        }
    }

    public void loadTimeline(@Nonnull UUID uuid, @Nonnull String name) {
        if (selectedTimeline != null) {
            timeline.save(uuid, selectedTimeline);
        }

        Path path = TimelineState.getTimelinePath(uuid, name);

        if (!Files.exists(path)) {
            timeline = new TimelineState();
            timeline.getProperties().put("camera", new CameraProperty());
            timeline.save(uuid, name);
        } else {
            Gson gson = ReplayPlugin.get().getGson();
            try {
                String json = Files.readString(path);
                timeline = gson.fromJson(json, TimelineState.class);

                timeline.getProperties().entrySet().removeIf(
                        entry -> entry.getValue() == null
                );
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        selectedTimeline = name;
        if (!timelines.contains(name)) {
            timelines.add(name);
        }

        commandsStack.clear();
    }

}
