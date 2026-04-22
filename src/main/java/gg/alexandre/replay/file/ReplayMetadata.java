package gg.alexandre.replay.file;

import com.google.gson.annotations.SerializedName;
import gg.alexandre.replay.util.Position;

import java.util.UUID;

public class ReplayMetadata {

    @SerializedName("uuid")
    public UUID uuid;

    @SerializedName("durationMs")
    public long durationMs;

    @SerializedName("ticks")
    public int ticks;

    @SerializedName("position")
    public Position position;

    public ReplayMetadata(long durationMs, int ticks, Position position) {
        this.uuid = UUID.randomUUID();
        this.durationMs = durationMs;
        this.ticks = ticks;
        this.position = position;
    }
}
