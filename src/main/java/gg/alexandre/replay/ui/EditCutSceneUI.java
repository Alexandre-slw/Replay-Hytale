package gg.alexandre.replay.ui;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.common.util.PathUtil;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import gg.alexandre.replay.ReplayPlugin;
import gg.alexandre.replay.cutscene.CutSceneMetadata;
import gg.alexandre.replay.replay.state.ReplayState;
import gg.alexandre.replay.repository.CutSceneRepository;
import gg.alexandre.replay.ui.codec.CodecConstructor;
import gg.alexandre.replay.ui.codec.UIKey;
import gg.alexandre.replay.ui.event.UIEventContext;
import gg.alexandre.replay.ui.event.UIEventHandler;
import gg.alexandre.replay.ui.event.UIEventIdData;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class EditCutSceneUI extends BaseUI<EditCutSceneUI.Data> {

    private final Path savePath;
    private final CutSceneMetadata metadata;
    private final boolean shouldDeleteOnCancel;

    @Nullable
    private final ReplayState state;

    private static final BuilderCodec<Data> CODEC = CodecConstructor.create(Data.class, Data::new);

    public static class Data extends UIEventIdData {
        @UIKey("@Name")
        private String name;

        @UIKey("@Duration")
        private double duration;
    }

    public EditCutSceneUI(@Nonnull PlayerRef playerRef, @Nonnull Path savePath, @Nonnull CutSceneMetadata metadata,
                          boolean shouldDeleteOnCancel, @Nullable ReplayState state) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, CODEC);

        this.savePath = savePath;
        this.metadata = metadata;
        this.shouldDeleteOnCancel = shouldDeleteOnCancel;
        this.state = state;
    }

    @Override
    public void init(@Nonnull UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.append("EditCutScene.ui");

        String name = savePath.getFileName().toString();
        if (name.endsWith(CutSceneRepository.CUTSCENE_EXTENSION)) {
            name = name.substring(0, name.length() - CutSceneRepository.CUTSCENE_EXTENSION.length());
        }
        uiCommandBuilder.set("#NameInput.Value", name);

        uiCommandBuilder.set("#DurationInput.Value", metadata.ticks / 30.0);
    }

    @Override
    public void register(@Nonnull UIEventBuilder uiEventBuilder, @Nonnull UIEventHandler<Data> eventHandler) {
        eventHandler.handle(
                CustomUIEventBindingType.ValueChanged,
                "#NameInput",
                (data) -> data.append("@Name", "#NameInput.Value"),
                this::onInputChange
        );

        eventHandler.handle(CustomUIEventBindingType.Activating,
                "#Discard",
                this::onDiscard
        );

        eventHandler.handle(
                CustomUIEventBindingType.Activating,
                "#Save",
                (data) -> data
                        .append("@Name", "#NameInput.Value")
                        .append("@Duration", "#DurationInput.Value"),
                this::onSave
        );
    }

    private void onInputChange(@Nonnull UIEventContext<Data> context) {
        String value = context.data.name;
        if (value != null) {
            value = value.trim();
            context.uiCommandBuilder.set("#Save.Disabled", isInvalidPath(value));
        }
    }

    private void onDiscard(@Nonnull UIEventContext<Data> context) {
        if (shouldDeleteOnCancel) {
            try {
                Files.delete(savePath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        context.close();
    }

    private void onSave(@Nonnull UIEventContext<Data> context) {
        String value = context.data.name;
        if (value == null) {
            return;
        }

        value = value.trim();
        if (isInvalidPath(value)) {
            return;
        }

        String name = value + CutSceneRepository.CUTSCENE_EXTENSION;
        Path path = savePath.getParent() != null ? savePath.getParent().resolve(name) : Path.of(name);
        try {
            Files.move(savePath, path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        metadata.ticks = (int) Math.round(context.data.duration * 30);
        ReplayPlugin.get().getCutSceneRepository().saveCutScene(path, metadata);

        context.close();

        if (shouldDeleteOnCancel) {
            ReplayPlugin.get().editCutScene(context.playerRef, path);
        }

        if (state != null) {
            state.path = path;
        }
    }

    private boolean isInvalidPath(@Nonnull String value) {
        String fileName = value + CutSceneRepository.CUTSCENE_EXTENSION;
        if (fileName.equals(savePath.getFileName().toString())) {
            return false;
        }

        return !PathUtil.isValidName(value) ||
               (savePath.getParent() != null && Files.exists(savePath.getParent().resolve(fileName)));
    }

}
