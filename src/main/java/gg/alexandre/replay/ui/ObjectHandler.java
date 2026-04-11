package gg.alexandre.replay.ui;


import com.google.gson.Gson;

import javax.annotation.Nonnull;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class ObjectHandler<Data, Type> implements BiConsumer<Data, String>, Function<Data, String> {
    private final Class<Type> typeClass;
    private final BiConsumer<Data, Type> dataConsumer;
    private final Function<Data, Type> dataSupplier;

    private final Gson gson = new Gson();

    public ObjectHandler(@Nonnull Class<Type> typeClass,
                   @Nonnull BiConsumer<Data, Type> dataConsumer,
                   @Nonnull Function<Data, Type> dataSupplier) {
        this.typeClass = typeClass;
        this.dataConsumer = dataConsumer;
        this.dataSupplier = dataSupplier;
    }

    @Override
    public void accept(Data data, String type) {
        Type value = gson.fromJson(type, typeClass);
        dataConsumer.accept(data, value);
    }

    @Override
    public String apply(Data data) {
        return gson.toJson(dataSupplier.apply(data));
    }
}