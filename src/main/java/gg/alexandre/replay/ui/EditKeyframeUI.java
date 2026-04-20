package gg.alexandre.replay.ui;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import gg.alexandre.replay.ui.codec.CodecConstructor;
import gg.alexandre.replay.ui.codec.UIKey;
import gg.alexandre.replay.ui.event.UIEventContext;
import gg.alexandre.replay.ui.event.UIEventHandler;
import gg.alexandre.replay.ui.event.UIEventIdData;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

public class EditKeyframeUI extends BaseUI<EditKeyframeUI.Data> {

    private static final BuilderCodec<Data> CODEC = CodecConstructor.create(Data.class, Data::new);

    public static class Data extends UIEventIdData {
        @UIKey("@Value")
        private int value;
    }

    private final String id;
    private final int min;
    private final int max;
    private final int value;
    private final Consumer<Integer> onSave;
    private final Runnable onRemove;

    public EditKeyframeUI(@Nonnull PlayerRef playerRef, @Nonnull String id, int min, int max, int value,
                          @Nonnull Consumer<Integer> onSave, @Nonnull Runnable onRemove) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, CODEC);

        this.id = id;
        this.min = min;
        this.max = max;
        this.value = value;
        this.onSave = onSave;
        this.onRemove = onRemove;
    }

    @Override
    public void init(@Nonnull UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.append("EditKeyframe.ui");

        uiCommandBuilder.set("#Slider.Min", min);
        uiCommandBuilder.set("#Slider.Max", max);
        uiCommandBuilder.set("#Slider.Value", value);

        uiCommandBuilder.set("#Name.Text", Message.translation("replay." + id));
    }

    @Override
    public void register(@Nonnull UIEventBuilder uiEventBuilder, @Nonnull UIEventHandler<Data> eventHandler) {
        eventHandler.handle(CustomUIEventBindingType.Activating,
                "#CloseButton",
                this::onClose
        );

        eventHandler.handle(CustomUIEventBindingType.Activating,
                "#Remove",
                this::onRemove
        );

        eventHandler.handle(
                CustomUIEventBindingType.Activating,
                "#Save",
                (data) -> data.append("@Value", "#Slider.Value"),
                this::onSave
        );
    }

    private void onClose(@Nonnull UIEventContext<Data> context) {
        context.close();
    }

    private void onRemove(@Nonnull UIEventContext<Data> context) {
        onRemove.run();
        context.close();
    }

    private void onSave(@Nonnull UIEventContext<Data> context) {
        onSave.accept(context.data.value);
        context.close();
    }

}
