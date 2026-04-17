package gg.alexandre.replay.gson;

import com.google.gson.*;
import gg.alexandre.replay.replay.editor.properties.base.BaseProperty;
import gg.alexandre.replay.replay.editor.registry.PropertyRegistry;

import java.lang.reflect.Type;

public final class BasePropertyAdapter implements JsonSerializer<BaseProperty<?>>, JsonDeserializer<BaseProperty<?>> {

    @Override
    public BaseProperty<?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();
        JsonElement typeElement = obj.get("id");
        if (typeElement == null) {
            throw new JsonParseException("Missing 'id' field for BaseProperty");
        }

        String id = typeElement.getAsString();
        Class<? extends BaseProperty<?>> clazz = PropertyRegistry.get().get(id);

        if (clazz == null) {
            throw new JsonParseException("Unknown property id: " + id);
        }

        return context.deserialize(obj, clazz);
    }

    @Override
    public JsonElement serialize(BaseProperty<?> src, Type typeOfSrc, JsonSerializationContext context) {
        return context.serialize(src, src.getClass());
    }
}