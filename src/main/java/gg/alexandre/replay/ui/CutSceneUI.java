package gg.alexandre.replay.ui;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import gg.alexandre.replay.ReplayPlugin;
import gg.alexandre.replay.cutscene.CutSceneMetadata;
import gg.alexandre.replay.recorder.ReplayRecorder;
import gg.alexandre.replay.repository.CutSceneRepository;
import gg.alexandre.replay.ui.codec.CodecConstructor;
import gg.alexandre.replay.ui.event.UIEventContext;
import gg.alexandre.replay.ui.event.UIEventHandler;
import gg.alexandre.replay.ui.event.UIEventIdData;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.List;

public class CutSceneUI extends BaseUI<CutSceneUI.Data> {

    private final CutSceneRepository cutSceneRepository;

    private static final BuilderCodec<Data> CODEC = CodecConstructor.create(Data.class, Data::new);

    public static class Data extends UIEventIdData {

    }

    public CutSceneUI(@Nonnull PlayerRef playerRef, @Nonnull CutSceneRepository cutSceneRepository,
                      @Nonnull ReplayRecorder recorder) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, CODEC);

        this.cutSceneRepository = cutSceneRepository;
    }

    @Override
    public void init(@Nonnull UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.append("CutScene.ui");

        List<Path> cutScenes = cutSceneRepository.getCutScenes(playerRef);
        for (int i = 0; i < cutScenes.size(); i++) {
            Path replay = cutScenes.get(i);
            uiCommandBuilder.append("#List", "ReplayEntry.ui");

            String name = replay.getFileName().toString();
            if (name.endsWith(CutSceneRepository.CUTSCENE_EXTENSION)) {
                name = name.substring(0, name.length() - CutSceneRepository.CUTSCENE_EXTENSION.length());
            }
            uiCommandBuilder.set("#List[" + i + "].Text", name);
        }

        if (cutScenes.isEmpty()) {
            uiCommandBuilder.appendInline("#List", """
                    Label {
                      Text: %replay.noCutScenesYet;
                      Style: (FontSize: 16, Alignment: Center);
                    }
                    """);
        }
    }

    @Override
    public void register(@Nonnull UIEventBuilder uiEventBuilder, @Nonnull UIEventHandler<Data> eventHandler) {
        eventHandler.handle(CustomUIEventBindingType.Activating,
                "#Create",
                this::onCreate
        );

        List<Path> cutScenes = cutSceneRepository.getCutScenes(playerRef);
        for (int i = 0; i < cutScenes.size(); i++) {
            Path replay = cutScenes.get(i);
            eventHandler.handle(CustomUIEventBindingType.Activating,
                    "#List[" + i + "]",
                    context -> onCutScene(context, replay)
            );
        }
    }

    private void onCutScene(@Nonnull UIEventContext<Data> context, @Nonnull Path path) {
        context.close();

        ReplayPlugin.get().editCutScene(playerRef, path);
    }

    private void onCreate(@Nonnull UIEventContext<Data> context) {
        Ref<EntityStore> ref = context.playerRef.getReference();
        assert ref != null;
        Store<EntityStore> store = ref.getStore();

        store.getExternalData().getWorld().execute(() -> {
            Player player = store.getComponent(ref, Player.getComponentType());
            assert player != null;

            Path path = cutSceneRepository.newCutScene(playerRef);
            CutSceneMetadata metadata = new CutSceneMetadata(30 * 5);

            cutSceneRepository.saveCutScene(path, metadata);

            player.getPageManager().openCustomPage(
                    ref, store, new EditCutSceneUI(playerRef, path, metadata, true)
            );
        });
    }

}
