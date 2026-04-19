package gg.alexandre.replay.replay.state;

import javax.annotation.Nullable;

public class UIState {

    public double timelineZoom = 1.0;

    public int draggingTick;
    public boolean dragging;

    public boolean controlGame;
    public boolean sentEscHint;

    public boolean dirtyTimeline;

    @Nullable
    public Keyframe selectedKeyframe;

    public record Keyframe(int propertyIndex, int tick) {
    }

}
