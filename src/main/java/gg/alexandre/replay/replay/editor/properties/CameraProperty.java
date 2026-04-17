package gg.alexandre.replay.replay.editor.properties;

import gg.alexandre.replay.replay.editor.interpolation.InterpolationUtil;
import gg.alexandre.replay.replay.editor.properties.base.BaseProperty;
import gg.alexandre.replay.replay.state.ReplayState;
import gg.alexandre.replay.util.Position;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import javax.annotation.Nonnull;
import java.util.Map;

public class CameraProperty extends BaseProperty<Position> {

    public CameraProperty() {
        super(null);
    }

    @Override
    public void handle(@Nonnull ReplayState state, int tick) {
        Position cameraPosition = getValue(tick);
        if (cameraPosition == null) {
            return;
        }

        state.edit.cameraPosition = cameraPosition;
    }

    @NullableDecl
    @Override
    public Position getValue(int tick) {
        Map.Entry<Integer, Position> previous = getValues().floorEntry(tick);
        Map.Entry<Integer, Position> next = getValues().ceilingEntry(tick);

        Map.Entry<Integer, Position> p0Entry = getValues().lowerEntry(previous != null ? previous.getKey() : tick);
        Map.Entry<Integer, Position> p3Entry = getValues().higherEntry(next != null ? next.getKey() : tick);

        if (previous == null && next == null) {
            return getDefaultValue();
        } else if (previous == null) {
            return next.getValue();
        } else if (next == null) {
            return previous.getValue();
        } else {
            int previousTick = previous.getKey();
            int nextTick = next.getKey();
            Position p0 = p0Entry != null ? p0Entry.getValue() : previous.getValue();
            Position p1 = previous.getValue();
            Position p2 = next.getValue();
            Position p3 = p3Entry != null ? p3Entry.getValue() : next.getValue();

            double ratio = (double) (tick - previousTick) / (nextTick - previousTick);

            double x = InterpolationUtil.catmullRom(p0.x(), p1.x(), p2.x(), p3.x(), ratio);
            double y = InterpolationUtil.catmullRom(p0.y(), p1.y(), p2.y(), p3.y(), ratio);
            double z = InterpolationUtil.catmullRom(p0.z(), p1.z(), p2.z(), p3.z(), ratio);
            double yaw = InterpolationUtil.catmullRom(p0.yaw(), p1.yaw(), p2.yaw(), p3.yaw(), ratio);
            double pitch = InterpolationUtil.catmullRom(p0.pitch(), p1.pitch(), p2.pitch(), p3.pitch(), ratio);

            return new Position(x, y, z, yaw, pitch);
        }
    }

    @Nonnull
    @Override
    public String id() {
        return "camera";
    }
}
