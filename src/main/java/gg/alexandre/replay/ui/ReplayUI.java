package gg.alexandre.replay.ui;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import gg.alexandre.replay.ReplayPlugin;
import gg.alexandre.replay.repository.ReplayRepository;
import gg.alexandre.replay.ui.codec.CodecConstructor;
import gg.alexandre.replay.ui.event.UIEventContext;
import gg.alexandre.replay.ui.event.UIEventHandler;
import gg.alexandre.replay.ui.event.UIEventIdData;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.List;

public class ReplayUI extends BaseUI<ReplayUI.Data> {

    private final ReplayRepository replayRepository;

    private static final BuilderCodec<Data> CODEC = CodecConstructor.create(Data.class, Data::new);

    public static class Data extends UIEventIdData {

    }

    public ReplayUI(@Nonnull PlayerRef playerRef, @Nonnull ReplayRepository replayRepository) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, CODEC);

        this.replayRepository = replayRepository;
    }

    @Override
    public void init(@Nonnull UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.append("ReplayUI.ui");

        List<Path> replays = replayRepository.getReplays(playerRef);
        for (int i = 0; i < replays.size(); i++) {
            Path replay = replays.get(i);
            uiCommandBuilder.append("#List", "ReplayEntry.ui");

            String name = replay.getFileName().toString();
            if (name.endsWith(ReplayRepository.REPLAY_EXTENSION)) {
                name = name.substring(0, name.length() - ReplayRepository.REPLAY_EXTENSION.length());
            }
            uiCommandBuilder.set("#List[" + i + "].Text", name);
        }
    }

    @Override
    public void register(@Nonnull UIEventBuilder uiEventBuilder, @Nonnull UIEventHandler<Data> eventHandler) {
        eventHandler.handle(CustomUIEventBindingType.Activating,
                "#Record",
                this::onRecord
        );

        List<Path> replays = replayRepository.getReplays(playerRef);
        for (int i = 0; i < replays.size(); i++) {
            Path replay = replays.get(i);
            eventHandler.handle(CustomUIEventBindingType.Activating,
                    "#List[" + i + "]",
                    _ -> {
                        ReplayPlugin.get().startReplaying(playerRef, replay);
                        close();
                    }
            );
        }
    }

    private void onRecord(@Nonnull UIEventContext<Data> context) {
        // TODO: stop recording
        ReplayPlugin.get().startRecording(playerRef);
        close();
    }

    private void onClose(@Nonnull UIEventContext<Data> context) {
        close();
    }

}
