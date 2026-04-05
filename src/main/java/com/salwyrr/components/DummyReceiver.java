package com.salwyrr.components;

import com.hypixel.hytale.protocol.ToClientPacket;
import com.hypixel.hytale.server.core.receiver.IPacketReceiver;

import javax.annotation.Nonnull;

public class DummyReceiver implements IPacketReceiver {

    @Override
    public void writeNoCache(@Nonnull ToClientPacket packet) {

    }

    @Override
    public void write(@Nonnull ToClientPacket packet) {

    }
}