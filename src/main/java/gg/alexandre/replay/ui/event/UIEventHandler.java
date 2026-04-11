package gg.alexandre.replay.ui.event;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.function.Consumer;

public class UIEventHandler<T extends UIEventIdData> implements UIEventConsumer<T> {

    private final HashMap<String, UIEventConsumer<T>> eventConsumers = new HashMap<>();

    private UIEventBuilder eventBuilder;

    @Nonnull
    public BuilderCodec.Builder<T> prepare(@Nonnull BuilderCodec.Builder<T> builder) {
        return builder.append(
                new KeyedCodec<>("EventId", Codec.STRING),
                UIEventIdData::setEventId, UIEventIdData::getEventId
        ).add();
    }

    public void bind(@Nullable UIEventBuilder eventBuilder) {
        this.eventBuilder = eventBuilder;
    }

    public void handle(@Nonnull CustomUIEventBindingType type, @Nonnull String selector,
                       @Nonnull UIEventConsumer<T> consumer) {
        handle(type, selector, null, consumer);
    }

    public void handle(@Nonnull CustomUIEventBindingType type, @Nonnull String selector,
                       @Nullable Consumer<EventData> eventDataConsumer, @Nonnull UIEventConsumer<T> consumer) {
        if (eventBuilder == null) {
            throw new IllegalStateException("Event builder not set");
        }

        String id = type + ":" + selector;
        if (eventConsumers.containsKey(id)) {
            throw new IllegalStateException("Event already registered for " + id);
        }

        eventConsumers.put(id, consumer);

        EventData data = EventData.of("EventId", id);
        if (eventDataConsumer != null) {
            eventDataConsumer.accept(data);
        }

        eventBuilder.addEventBinding(
                type,
                selector,
                data
        );
    }

    @Override
    public void handleEvent(@Nonnull UIEventContext<T> context) {
        String clickId = context.data().getEventId();
        if (clickId == null) {
            return;
        }

        UIEventConsumer<T> consumer = eventConsumers.get(clickId);
        if (consumer != null) {
            consumer.handleEvent(context);
        }

        context.data().setEventId(null);
    }

}
