package gg.alexandre.replay.replay.editor.properties;

import com.hypixel.hytale.server.core.entity.entities.Player;
import gg.alexandre.replay.replay.editor.interpolation.InterpolationUtil;
import gg.alexandre.replay.replay.editor.properties.base.BaseProperty;
import gg.alexandre.replay.replay.state.ReplayState;
import gg.alexandre.replay.ui.event.UIEventContext;
import gg.alexandre.replay.util.Position;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

public class CameraProperty extends BaseProperty<Position> {

    public CameraProperty() {
        super(new Position(0, 0, 0, 0, 0));
    }

    @Override
    public void handle(@Nonnull ReplayState state, int tick) {
        Position cameraPosition = getValue(tick);
        if (cameraPosition == null) {
            return;
        }

        state.edit.cameraPosition = cameraPosition;
    }

    @Nullable
    @Override
    public Position getValue(int tick) {
        Map.Entry<Integer, Position> previous = getValues().floorEntry(tick);
        if (previous == null) {
            return null;
        }

        Map.Entry<Integer, Position> next = getValues().higherEntry(tick);

        Map.Entry<Integer, Position> p0Entry = getValues().lowerEntry(previous != null ? previous.getKey() : tick);
        Map.Entry<Integer, Position> p3Entry = getValues().higherEntry(next != null ? next.getKey() : tick);

        if (next == null) {
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

    @Override
    public void editKeyframe(@Nonnull ReplayState state, @Nonnull Player player,
                             @Nonnull UIEventContext<?> context, int tick) {
        // TODO: edit camera
    }

    @Nonnull
    @Override
    public String id() {
        return "camera";
    }

    @Nonnull
    @Override
    public Position getDefaultValue(@Nonnull ReplayState state) {
        return state.edit.cameraPosition;
    }
}
