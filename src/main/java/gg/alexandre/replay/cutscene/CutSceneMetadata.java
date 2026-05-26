package gg.alexandre.replay.cutscene;

import com.google.gson.annotations.SerializedName;

import java.util.UUID;

public class CutSceneMetadata {

    @SerializedName("uuid")
    public UUID uuid;

    @SerializedName("ticks")
    public int ticks;

    public CutSceneMetadata(int ticks) {
        this.uuid = UUID.randomUUID();
        this.ticks = ticks;
    }
}
