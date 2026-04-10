package gg.alexandre.replay.file;

import gg.alexandre.replay.protocol.ReplayPacket;
import gg.alexandre.replay.protocol.ReplayProtocol;
import gg.alexandre.replay.protocol.packets.TickReplayPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import javax.annotation.Nonnull;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.zip.ZipFile;

public class ReplayInputFile {

    private final ReplayProtocol protocol;

    private final ZipFile zipFile;
    private final DataInputStream packetsInputStream;

    private ReplayPacket packet;

    public ReplayInputFile(@Nonnull Path path, @Nonnull ReplayProtocol protocol) throws IOException {
        this.protocol = protocol;

        zipFile = new ZipFile(path.toFile());

        packetsInputStream = createInputStream("packets.dat");
    }

    private DataInputStream createInputStream(@Nonnull String name) {
        try {
            return new DataInputStream(new BufferedInputStream(zipFile.getInputStream(zipFile.getEntry(name))));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized void consumeConfigPhase(@Nonnull Consumer<ReplayPacket> consumer) {
        DataInputStream stream = createInputStream("config.dat");
        try {
            Optional<ReplayPacket> packet;
            while ((packet = read(stream)).isPresent()) {
                consumer.accept(packet.get());
            }

            stream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized boolean read(int tick) {
        try {
            if (packet == null) {
                packet = read(packetsInputStream).orElse(null);
            }

            if (packet instanceof TickReplayPacket tickPacket) {
                if (tickPacket.getTick() > tick) {
                    return false;
                }
            }

            return packet != null;
        } catch (IOException e) {
            return false;
        }
    }

    public synchronized ReplayPacket consumePacket() {
        try {
            return packet;
        } finally {
            packet = null;
        }
    }

    private synchronized Optional<ReplayPacket> read(@Nonnull DataInputStream stream) throws IOException {
        ByteBuf in;
        int packetId;
        try {
            packetId = stream.readInt();
            int length = stream.readInt();

            byte[] data = new byte[length];
            stream.readFully(data);

            in = Unpooled.buffer(length);
            in.writeBytes(data);
        } catch (IOException e) {
            return Optional.empty();
        }

        ReplayPacket packet = protocol.getInstance(packetId);
        packet.deserialize(in);
        return Optional.of(packet);
    }

    public boolean isEndOfFile() {
        try {
            return packetsInputStream.available() == 0;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized void close() throws IOException {
        packetsInputStream.close();
        zipFile.close();
    }

}
