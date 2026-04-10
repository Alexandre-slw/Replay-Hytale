package gg.alexandre.replay.replay.state;

import gg.alexandre.replay.file.ReplayInputFile;

import java.util.UUID;

public class ReplayState {

    public ReplayInputFile file;

    public int tick;
    public boolean hasStarted;
    public boolean isPlaying;
    public boolean isFilteringPackets;
    public boolean isProcessingPackets;
    public boolean sentJoinWorld;
    public boolean clearedWorld;
    public UUID playerUuid;

}
