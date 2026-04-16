package gg.alexandre.replay.replay.state;

import gg.alexandre.replay.file.ReplayInputFile;

import java.nio.file.Path;
import java.util.UUID;

public class ReplayState {

    public Path replayPath;
    public ReplayInputFile file;

    public int tick;
    public boolean hasStarted;
    public boolean isPlaying;
    public boolean isFilteringPackets;
    public boolean isProcessingPackets;
    public boolean sentJoinWorld;
    public boolean clearedWorld;
    public UUID playerUuid;

    public String lang;
    public boolean sentTranslations;

    public boolean controlGame;
    public boolean sentEscHint;

}
