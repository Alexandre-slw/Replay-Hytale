package gg.alexandre.replay.file;

import com.google.gson.annotations.SerializedName;

public class ReplayMetadata {

    @SerializedName("durationMs")
    public long durationMs;

    @SerializedName("ticks")
    public int ticks;

    public ReplayMetadata(long durationMs, int ticks) {
        this.durationMs = durationMs;
        this.ticks = ticks;
    }
}
