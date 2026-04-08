package com.salwyrr.file;

import com.hypixel.hytale.server.core.util.io.FileUtil;
import com.salwyrr.protocol.ReplayPacket;
import com.salwyrr.protocol.ReplayProtocol;
import com.salwyrr.protocol.packets.EndSnapshotReplayPacket;
import com.salwyrr.protocol.packets.StartSnapshotReplayPacket;
import com.salwyrr.protocol.packets.TickReplayPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ReplayOutputFile {

    private static final Path RECORD_DIRECTORY = Path.of(".record");

    private final Path savePath;
    private final ReplayProtocol protocol;

    private final Path recordPath;

    private final DataOutputStream packetsOutputStream;
    private DataOutputStream writeOutputStream;

    private int tick = -1;

    private final List<String> files = new ArrayList<>();

    public ReplayOutputFile(Path savePath, ReplayProtocol protocol) throws IOException {
        this.savePath = savePath;
        this.protocol = protocol;

        recordPath = RECORD_DIRECTORY.resolve(savePath.getFileName());

        if (Files.exists(recordPath)) {
            FileUtil.deleteDirectory(recordPath);
        }

        Files.createDirectories(recordPath);

        packetsOutputStream = createOutputStream("packets.dat");
        writeOutputStream = packetsOutputStream;
    }

    private DataOutputStream createOutputStream(String name) {
        try {
            files.add(name);
            return new DataOutputStream(new BufferedOutputStream(new FileOutputStream(
                    recordPath.resolve(name).toFile()
            )));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized void startSnapshot(int tick) {
        write(new StartSnapshotReplayPacket(), tick);
    }

    public synchronized void endSnapshot(int tick) {
        write(new EndSnapshotReplayPacket(), tick);
    }

    public synchronized void configPhase(Runnable runnable) {
        writeOutputStream = createOutputStream("config.dat");
        try {
            runnable.run();
            writeOutputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            writeOutputStream = packetsOutputStream;
        }
    }

    public synchronized void write(ReplayPacket packet, int tick) {
        if (this.tick != tick) {
            this.tick = tick;
            write(new TickReplayPacket(tick), tick);
        }

        try {
            ByteBuf buffer = Unpooled.buffer();
            packet.serialize(buffer);

            writeOutputStream.writeInt(protocol.getId(packet));
            writeOutputStream.writeInt(buffer.readableBytes());
            buffer.readBytes(writeOutputStream, buffer.readableBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized void close() throws IOException {
        writeOutputStream.close();
        if (writeOutputStream != packetsOutputStream) {
            packetsOutputStream.close();
        }

        if (savePath.getParent() != null) {
            Files.createDirectories(savePath.getParent());
        }

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(
                savePath.toFile()
        )))) {
            for (String file : files) {
                FileInputStream fileInputStream = new FileInputStream(recordPath.resolve(file).toFile());
                zipOutputStream.putNextEntry(new ZipEntry(file));
                fileInputStream.transferTo(zipOutputStream);
                zipOutputStream.closeEntry();
                fileInputStream.close();
            }
        }

        FileUtil.deleteDirectory(recordPath);
    }

}
