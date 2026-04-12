package gg.alexandre.replay.ui.codec;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import gg.alexandre.replay.ui.event.UIEventIdData;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.function.Supplier;

public class CodecConstructor {

    @Nonnull
    public static <T extends UIEventIdData> BuilderCodec<T> create(@Nonnull Class<T> type, @Nonnull Supplier<T> supplier) {
        BuilderCodec.Builder<T> builder = BuilderCodec.builder(type, supplier);

        for (Field field : type.getDeclaredFields()) {
            if (field.isAnnotationPresent(UIKey.class)) {
                UIKey annotation = field.getAnnotation(UIKey.class);
                try {
                    field.setAccessible(true);

                    builder.append(
                            new KeyedCodec(annotation.value(), codecForField(field.getType())),
                            (data, v) -> {
                                try {
                                    field.set(data, v);
                                } catch (IllegalAccessException e) {
                                    throw new RuntimeException("Failed to set field: " + field.getName(), e);
                                }
                            },
                            data -> {
                                try {
                                    return field.get(data);
                                } catch (IllegalAccessException e) {
                                    throw new RuntimeException("Failed to get field: " + field.getName(), e);
                                }
                            }
                    ).add();
                } catch (Exception e) {
                    throw new RuntimeException("Failed to create codec for field: " + field.getName(), e);
                }
            }
        }

        builder.append(
                new KeyedCodec<>("EventId", Codec.STRING),
                UIEventIdData::setEventId, UIEventIdData::getEventId
        ).add();

        return builder.build();
    }

    private static <T> Codec<T> codecForField(@Nonnull Class<T> fieldType) {
        if (fieldType == String.class) {
            return (Codec<T>) Codec.STRING;
        } else if (fieldType == Integer.class || fieldType == int.class) {
            return (Codec<T>) Codec.INTEGER;
        } else if (fieldType == Boolean.class || fieldType == boolean.class) {
            return (Codec<T>) Codec.BOOLEAN;
        } else if (fieldType == Double.class || fieldType == double.class) {
            return (Codec<T>) Codec.DOUBLE;
        } else if (fieldType == Float.class || fieldType == float.class) {
            return (Codec<T>) Codec.FLOAT;
        } else if (fieldType == Long.class || fieldType == long.class) {
            return (Codec<T>) Codec.LONG;
        } else if (fieldType == Short.class || fieldType == short.class) {
            return (Codec<T>) Codec.SHORT;
        } else if (fieldType == Byte.class || fieldType == byte.class) {
            return (Codec<T>) Codec.BYTE;
        } else {
            return new ObjectCodec<>(fieldType);
        }
    }

}
