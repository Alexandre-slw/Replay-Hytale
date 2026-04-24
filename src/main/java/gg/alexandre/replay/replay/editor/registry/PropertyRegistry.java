package gg.alexandre.replay.replay.editor.registry;

import gg.alexandre.replay.replay.editor.properties.CameraProperty;
import gg.alexandre.replay.replay.editor.properties.FovProperty;
import gg.alexandre.replay.replay.editor.properties.SpeedProperty;
import gg.alexandre.replay.replay.editor.properties.base.BaseProperty;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class PropertyRegistry {

    private static final PropertyRegistry INSTANCE = new PropertyRegistry();

    private final Map<String, Class<? extends BaseProperty<?>>> registry = new HashMap<>();

    private PropertyRegistry() {
        register(CameraProperty.class);
        register(SpeedProperty.class);
        register(FovProperty.class);
    }

    private void register(Class<? extends BaseProperty<?>> clazz) {
        try {
            BaseProperty<?> baseProperty = clazz.getDeclaredConstructor().newInstance();
            registry.put(baseProperty.id(), clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to register property: " + clazz.getSimpleName(), e);
        }
    }

    @Nullable
    public Class<? extends BaseProperty<?>> get(String id) {
        return registry.get(id);
    }

    @Nullable
    public BaseProperty<?> create(String id) {
        Class<? extends BaseProperty<?>> clazz = get(id);
        if (clazz == null) {
            return null;
        }

        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create property instance for id: " + id, e);
        }
    }

    @Nonnull
    public static PropertyRegistry get() {
        return INSTANCE;
    }

    @Nonnull
    public Map<String, Class<? extends BaseProperty<?>>> getRegistry() {
        return registry;
    }
}
