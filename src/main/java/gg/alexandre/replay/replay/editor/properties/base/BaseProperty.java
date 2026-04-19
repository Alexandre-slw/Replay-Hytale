package gg.alexandre.replay.replay.editor.properties.base;

import com.google.gson.annotations.SerializedName;
import gg.alexandre.replay.replay.state.ReplayState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.TreeMap;

public abstract class BaseProperty<T> {

    @SerializedName("values")
    private final TreeMap<Integer, T> values = new TreeMap<>();

    @SerializedName("id")
    private final String id = id();

    private final T defaultValue;

    public BaseProperty(@Nonnull T defaultValue) {
        this.defaultValue = defaultValue;
    }

    public abstract void handle(@Nonnull ReplayState state, int tick);

    @Nullable
    public abstract T getValue(int tick);

    @Nonnull
    public abstract String id();

    @Nonnull
    public T getDefaultValue(@Nonnull ReplayState state) {
        return defaultValue;
    }

    @Nonnull
    public TreeMap<Integer, T> getValues() {
        return values;
    }
}
