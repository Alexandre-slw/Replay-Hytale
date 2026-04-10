package gg.alexandre.replay.util;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.NetworkChannel;
import com.hypixel.hytale.protocol.io.netty.ProtocolUtil;
import com.hypixel.hytale.server.core.auth.PlayerAuthentication;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.io.netty.NettyUtil;
import com.hypixel.hytale.server.core.modules.entity.component.Intangible;
import com.hypixel.hytale.server.core.modules.entity.component.Invulnerable;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.hitboxcollision.HitboxCollision;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import gg.alexandre.replay.ReplayPlugin;
import gg.alexandre.replay.components.TargetWatcherTag;
import io.netty.channel.embedded.EmbeddedChannel;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DummyUtil {

    public static String NAME_PREFIX = "ReplayRecorder_";

    @Nonnull
    public static CompletableFuture<PlayerRef> spawnDummyWatcher(@Nonnull PlayerRef targetPlayer, @Nonnull UUID uuid) {
        Player player = targetPlayer.getReference().getStore().getComponent(targetPlayer.getReference(), Player.getComponentType());

        EmbeddedChannel dummyChannel = new EmbeddedChannel();
        dummyChannel.attr(ProtocolUtil.STREAM_CHANNEL_KEY).set(NetworkChannel.Default);
        NettyUtil.TimeoutContext.init(dummyChannel, "play", "");

        String name = NAME_PREFIX + targetPlayer.getUsername();

        return Universe.get().addPlayer(
                dummyChannel,
                targetPlayer.getLanguage(),
                targetPlayer.getPacketHandler().getProtocolVersion(),
                uuid,
                name,
                new PlayerAuthentication(uuid, name),
                player.getClientViewRadius(),
                null
        ).thenApply(playerRef -> {
            Ref<EntityStore> ref = playerRef.getReference();

            ref.getStore().putComponent(ref, ReplayPlugin.TAG_TYPE, new TargetWatcherTag(targetPlayer.getReference()));
            makeGhost(ref.getStore(), ref);

            return playerRef;
        });
    }

    public static void makeGhost(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> playerRef) {
        store.tryRemoveComponent(playerRef, ModelComponent.getComponentType());
        store.tryRemoveComponent(playerRef, PlayerSkinComponent.getComponentType());
        store.tryRemoveComponent(playerRef, HitboxCollision.getComponentType());
        store.ensureComponent(playerRef, Intangible.getComponentType());
        store.ensureComponent(playerRef, Invulnerable.getComponentType());
    }

}
