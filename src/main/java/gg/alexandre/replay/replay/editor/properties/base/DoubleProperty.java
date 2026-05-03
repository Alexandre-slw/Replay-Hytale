package gg.alexandre.replay.replay.editor.properties.base;

import gg.alexandre.replay.replay.editor.interpolation.InterpolationUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

public abstract class DoubleProperty extends BaseProperty<Double> {

    public DoubleProperty(@Nonnull Double defaultValue) {
        super(defaultValue);
    }

    @Nullable
    @Override
    public Double getValue(double tick) {
        int intTick = (int) Math.floor(tick);

        Map.Entry<Integer, Double> previous = getValues().floorEntry(intTick);
        if (previous == null) {
            return null;
        }

        Map.Entry<Integer, Double> next = getValues().higherEntry(intTick);

        if (next == null) {
            return previous.getValue();
        } else {
            int previousTick = previous.getKey();
            int nextTick = next.getKey();
            Double previousValue = previous.getValue();
            Double nextValue = next.getValue();

            double ratio = (tick - previousTick) / (nextTick - previousTick);
            return InterpolationUtil.easeInOut(previousValue, nextValue, ratio);
        }
    }
}
