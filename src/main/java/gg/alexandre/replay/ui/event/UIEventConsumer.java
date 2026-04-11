package gg.alexandre.replay.ui.event;

import javax.annotation.Nonnull;

@FunctionalInterface
public interface UIEventConsumer<T extends UIEventIdData> {

    void handleEvent(@Nonnull UIEventContext<T> context);

}
