package gg.alexandre.replay.volumes;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEffect;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import gg.alexandre.replay.ReplayPlugin;
import gg.alexandre.replay.cutscene.CutSceneCodec;
import gg.alexandre.replay.cutscene.CutSceneMetadata;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CutSceneEffect extends TriggerEffect {

    @Nonnull
    public static final BuilderCodec<CutSceneEffect> CODEC = BuilderCodec.builder(
                    CutSceneEffect.class, CutSceneEffect::new, BASE_CODEC
            )
            .append(
                    new KeyedCodec<>("Play", Codec.BOOLEAN, false),
                    (e, v) -> e.play = v,
                    (e) -> e.play
            ).add()
            .append(
                    new KeyedCodec<>("Data", Codec.STRING, false),
                    (e, v) -> e.data = v,
                    (e) -> e.data
            ).add()
            .build();

    private boolean play = true;

    @Nullable
    private String data;

    public void execute(@Nonnull TriggerContext context) {
        Ref<EntityStore> entityRef = context.getEntityRef();
        Store<EntityStore> store = context.getStore();

        PlayerRef playerRef = store.getComponent(entityRef, PlayerRef.getComponentType());
        if (playerRef != null) {
            if (ReplayPlugin.get().isEditingCutScene(playerRef)) {
                return;
            }

            if (play && data != null) {
                CutSceneCodec.Data cutSceneData = CutSceneCodec.fromDataString(data);
                CutSceneMetadata metadata = new CutSceneMetadata(cutSceneData.ticks());

                ReplayPlugin.get().startCutScene(playerRef, cutSceneData.state(), metadata);
            }

            if (!play) {
                ReplayPlugin.get().stopCutScene(playerRef);
            }
        }
    }

}
