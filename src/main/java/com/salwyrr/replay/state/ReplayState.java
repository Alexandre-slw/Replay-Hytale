package com.salwyrr.replay.state;

import java.util.UUID;

public class ReplayState {

    public int tick;
    public boolean isPlaying;
    public boolean isFilteringPackets;
    public boolean isProcessingPackets;
    public boolean sentJoinWorld;
    public UUID playerUuid;

}
