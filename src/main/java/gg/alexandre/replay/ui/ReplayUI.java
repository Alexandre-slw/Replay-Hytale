package gg.alexandre.replay.ui;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Constants;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import gg.alexandre.replay.ReplayPlugin;
import gg.alexandre.replay.recorder.RecordingData;
import gg.alexandre.replay.recorder.ReplayRecorder;
import gg.alexandre.replay.repository.ReplayRepository;
import gg.alexandre.replay.ui.codec.CodecConstructor;
import gg.alexandre.replay.ui.event.UIEventContext;
import gg.alexandre.replay.ui.event.UIEventHandler;
import gg.alexandre.replay.ui.event.UIEventIdData;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class ReplayUI extends BaseUI<ReplayUI.Data> {

    private final ReplayRepository replayRepository;
    private final ReplayRecorder recorder;

    private boolean recording;

    private static final BuilderCodec<Data> CODEC = CodecConstructor.create(Data.class, Data::new);

    public static class Data extends UIEventIdData {

    }

    public ReplayUI(@Nonnull PlayerRef playerRef, @Nonnull ReplayRepository replayRepository,
                    @Nonnull ReplayRecorder recorder) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, CODEC);

        this.replayRepository = replayRepository;
        this.recorder = recorder;
    }

    @Override
    public void init(@Nonnull UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.append("Replay.ui");

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

        if (replays.isEmpty()) {
            uiCommandBuilder.appendInline("#List", """
                    Label {
                      Text: %replay.noReplaysYet;
                      Style: (FontSize: 16, Alignment: Center);
                    }
                    """);
        }

        RecordingData recordingData = recorder.getRecordingData(playerRef);
        if (recordingData != null) {
            recording = true;
            uiCommandBuilder.set("#Record.Text", Message.translation("replay.stopRecording"));

            Duration duration = recordingData.start.until(Instant.now());
            String formatted = String.format("%02d:%02d", duration.toMinutesPart(), duration.toSecondsPart());
            if (duration.toHoursPart() > 0) {
                formatted = String.format("%d:%s", duration.toHours(), formatted);
            }

            uiCommandBuilder.set(
                    "#Time.Text", Message.translation("replay.recording").getAnsiMessage() + " " + formatted
            );
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
                    context -> onReplay(context, replay)
            );
        }
    }

    private void onReplay(@Nonnull UIEventContext<Data> context, @Nonnull Path replay) {
        if (Constants.SINGLEPLAYER) {
            context.store.getExternalData().getWorld().execute(() -> {
                Player playerComponent = context.store.getComponent(context.ref, Player.getComponentType());
                assert playerComponent != null;
                playerComponent.getPageManager().openCustomPage(
                        context.ref, context.store, new SingleplayerWarningUI(playerRef, replay)
                );
            });
        } else {
            ReplayPlugin.get().startReplaying(playerRef, replay);
            context.close();
        }
    }

    private void onRecord(@Nonnull UIEventContext<Data> context) {
        context.close();

        Ref<EntityStore> ref = context.playerRef.getReference();
        assert ref != null;
        Store<EntityStore> store = ref.getStore();

        store.getExternalData().getWorld().execute(() -> {
            if (recording) {
                ReplayPlugin.get().stopRecording(playerRef);
            } else {
                ReplayPlugin.get().startRecording(playerRef);
            }
        });
    }

}
