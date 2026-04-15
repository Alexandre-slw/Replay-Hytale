package gg.alexandre.replay.ui.codec;


import com.google.gson.Gson;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.function.FunctionCodec;
import gg.alexandre.replay.ReplayPlugin;

import javax.annotation.Nonnull;

public class ObjectCodec<Type> extends FunctionCodec<String, Type> {

    public ObjectCodec(@Nonnull Class<Type> typeClass) {
        Gson gson = ReplayPlugin.get().getGson();
        super(Codec.STRING, (v) -> gson.fromJson(v, typeClass), gson::toJson);
    }

}