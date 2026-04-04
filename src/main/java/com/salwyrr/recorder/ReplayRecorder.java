package com.salwyrr.recorder;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.io.PacketIO;
import com.hypixel.hytale.protocol.io.PacketStatsRecorder;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.*;

public class ReplayRecorder {

    private boolean recording;

    // TODO: select file
    private String outputFile = "test.replay";
    private DataOutputStream outputStream;

    private final HytaleLogger logger = HytaleLogger.forEnclosingClass();

    public ReplayRecorder() {
        // TODO: snapshot
        //start();
    }

    private void write(Packet packet) {
        if (outputStream == null) {
            return;
        }

        ByteBuf buffer = Unpooled.buffer();
        PacketIO.writeFramedPacket(packet, packet.getClass(), buffer, PacketStatsRecorder.NOOP);

        try {
            outputStream.writeInt(packet.getId());
            outputStream.writeInt(buffer.readableBytes());
            buffer.readBytes(outputStream, buffer.readableBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void start() {
        stop();

        try {
            File file = new File(outputFile);
            System.out.println(file.getAbsoluteFile());
            outputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
        } catch (java.io.IOException e) {
            e.printStackTrace();
            return;
        }

        recording = true;

        logger.atInfo().log("Started recording");
    }

    public void stop() {
        if (!recording) {
            return;
        }

        recording = false;

        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        logger.atInfo().log("Stopped recording");
    }

    public void registerPacketCounters() {
        // TODO: record based on player
        PacketAdapters.registerOutbound((PacketHandler handler, Packet packet) -> {
            if (!recording) {
                return;
            }

            write(packet);
        });
    }

}
