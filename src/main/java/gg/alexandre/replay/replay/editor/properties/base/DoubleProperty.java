package gg.alexandre.replay.replay.editor.properties.base;

import gg.alexandre.replay.replay.editor.interpolation.InterpolationUtil;

import javax.annotation.Nullable;
import java.util.Map;

public abstract class DoubleProperty extends BaseProperty<Double> {

    public DoubleProperty(@Nullable Double defaultValue) {
        super(defaultValue);
    }

    @Nullable
    @Override
    public Double getValue(int tick) {
        Map.Entry<Integer, Double> previous = getValues().floorEntry(tick);
        Map.Entry<Integer, Double> next = getValues().ceilingEntry(tick);

        if (previous == null && next == null) {
            return getDefaultValue();
        } else if (previous == null) {
            return next.getValue();
        } else if (next == null) {
            return previous.getValue();
        } else {
            int previousTick = previous.getKey();
            int nextTick = next.getKey();
            Double previousValue = previous.getValue();
            Double nextValue = next.getValue();

            double ratio = (double) (tick - previousTick) / (nextTick - previousTick);
            return InterpolationUtil.easeInOut(previousValue, nextValue, ratio);
        }
    }
}
