package gg.alexandre.replay.ui;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.common.util.PathUtil;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import gg.alexandre.replay.repository.ReplayRepository;
import gg.alexandre.replay.ui.codec.CodecConstructor;
import gg.alexandre.replay.ui.codec.UIKey;
import gg.alexandre.replay.ui.event.UIEventContext;
import gg.alexandre.replay.ui.event.UIEventHandler;
import gg.alexandre.replay.ui.event.UIEventIdData;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SaveUI extends BaseUI<SaveUI.Data> {

    private final Path savePath;

    private static final BuilderCodec<Data> CODEC = CodecConstructor.create(Data.class, Data::new);

    public static class Data extends UIEventIdData {
        @UIKey("@Input")
        private String value;
    }

    public SaveUI(@Nonnull PlayerRef playerRef, @Nonnull Path savePath) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, CODEC);

        this.savePath = savePath;
    }

    @Override
    public void init(@Nonnull UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.append("SaveUI.ui");

        String name = savePath.getFileName().toString();
        if (name.endsWith(ReplayRepository.REPLAY_EXTENSION)) {
            name = name.substring(0, name.length() - ReplayRepository.REPLAY_EXTENSION.length());
        }
        uiCommandBuilder.set("#Input.Value", name);
    }

    @Override
    public void register(@Nonnull UIEventBuilder uiEventBuilder, @Nonnull UIEventHandler<Data> eventHandler) {
        eventHandler.handle(
                CustomUIEventBindingType.ValueChanged,
                "#Input",
                (data) -> data.append("@Input", "#Input.Value"),
                this::onInputChange
        );

        eventHandler.handle(CustomUIEventBindingType.Activating,
                "#Discard",
                this::onDiscard
        );

        eventHandler.handle(
                CustomUIEventBindingType.Activating,
                "#Save",
                (data) -> data.append("@Input", "#Input.Value"),
                this::onSave
        );
    }

    private void onInputChange(@Nonnull UIEventContext<Data> context) {
        String value = context.data().value;
        if (value != null) {
            value = value.trim();
            context.uiCommandBuilder().set("#Save.Disabled", isInvalidPath(value));
        }
    }

    private void onDiscard(@Nonnull UIEventContext<Data> context) {
        try {
            Files.delete(savePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        close();
    }

    private void onSave(@Nonnull UIEventContext<Data> context) {
        String value = context.data().value;
        if (value == null) {
            return;
        }

        value = value.trim();
        if (isInvalidPath(value)) {
            return;
        }

        String name = value + ReplayRepository.REPLAY_EXTENSION;
        Path path = savePath.getParent() != null ? savePath.getParent().resolve(name) : Path.of(name);
        try {
            Files.move(savePath, path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        close();
    }

    private boolean isInvalidPath(@Nonnull String value) {
        if ((value + ReplayRepository.REPLAY_EXTENSION).equals(savePath.getFileName().toString())) {
            return false;
        }

        return !PathUtil.isValidName(value) ||
                (savePath.getParent() != null &&
                        Files.exists(savePath.getParent().resolve(value + ReplayRepository.REPLAY_EXTENSION)));
    }

}
