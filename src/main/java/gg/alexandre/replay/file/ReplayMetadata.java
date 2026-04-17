package gg.alexandre.replay.file;

import com.google.gson.annotations.SerializedName;

import java.util.UUID;

public class ReplayMetadata {

    @SerializedName("uuid")
    public UUID uuid;

    @SerializedName("durationMs")
    public long durationMs;

    @SerializedName("ticks")
    public int ticks;

    public ReplayMetadata(long durationMs, int ticks) {
        this.uuid = UUID.randomUUID();
        this.durationMs = durationMs;
        this.ticks = ticks;
    }
}
