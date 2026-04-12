package gg.alexandre.replay.file;

import com.hypixel.hytale.server.core.util.io.FileUtil;
import gg.alexandre.replay.ReplayPlugin;
import gg.alexandre.replay.protocol.ReplayPacket;
import gg.alexandre.replay.protocol.ReplayProtocol;
import gg.alexandre.replay.protocol.packets.EndSnapshotReplayPacket;
import gg.alexandre.replay.protocol.packets.StartSnapshotReplayPacket;
import gg.alexandre.replay.protocol.packets.TickReplayPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import javax.annotation.Nonnull;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ReplayOutputFile {

    private static final Path RECORD_DIRECTORY = ReplayPlugin.get().getDataDirectory().resolve(".record");

    private final Path savePath;
    private final ReplayProtocol protocol;

    private final Path recordPath;

    private final DataOutputStream packetsOutputStream;
    private DataOutputStream writeOutputStream;

    private int tick = -1;

    private final List<String> files = new ArrayList<>();

    public ReplayOutputFile(@Nonnull Path savePath, @Nonnull ReplayProtocol protocol) throws IOException {
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

    @Nonnull
    private DataOutputStream createOutputStream(@Nonnull String name) {
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

    public synchronized void configPhase(@Nonnull Runnable runnable) {
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

    public synchronized void write(@Nonnull ReplayPacket packet, int tick) {
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

    @Nonnull
    public Path getSavePath() {
        return savePath;
    }

}
