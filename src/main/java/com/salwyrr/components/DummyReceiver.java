package com.salwyrr.components;

import com.hypixel.hytale.protocol.EntityUpdate;
import com.hypixel.hytale.protocol.ToClientPacket;
import com.hypixel.hytale.protocol.packets.entities.EntityUpdates;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.receiver.IPacketReceiver;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

public class DummyReceiver implements IPacketReceiver {

    private final PlayerRef playerRef;
    private final Consumer<ToClientPacket> consumer;

    public DummyReceiver(PlayerRef playerRef, Consumer<ToClientPacket> consumer) {
        this.playerRef = playerRef;
        this.consumer = consumer;
    }

    @Override
    public void writeNoCache(@Nonnull ToClientPacket packet) {
        handle(packet);
    }

    @Override
    public void write(@Nonnull ToClientPacket packet) {
        handle(packet);
    }

    private void handle(@Nonnull ToClientPacket packet) {
        if (packet instanceof EntityUpdates entityUpdates && entityUpdates.updates != null) {
            for (EntityUpdate update : entityUpdates.updates) {
                NetworkId networkId = playerRef.getReference().getStore().getComponent(
                        playerRef.getReference(), NetworkId.getComponentType()
                );
                if (update.networkId == networkId.getId()) {
                    update.networkId = 0;
                }
            }
        }

        consumer.accept(packet);
    }
}