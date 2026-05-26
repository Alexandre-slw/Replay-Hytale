package gg.alexandre.replay.cutscene.util;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class ByteBufferUtil {

    public static void writeSignedVarInt(ByteArrayOutputStream out, int value) {
        writeVarInt(out, zigZagEncode(value));
    }

    public static int readSignedVarInt(ByteBuffer buffer) {
        return zigZagDecode(readVarInt(buffer));
    }

    private static int zigZagEncode(int value) {
        return (value << 1) ^ (value >> 31);
    }

    private static int zigZagDecode(int value) {
        return (value >>> 1) ^ -(value & 1);
    }

    public static void writeVarInt(ByteArrayOutputStream out, int value) {
        if (value < 0) {
            throw new IllegalArgumentException("VarInt cannot be negative: " + value);
        }

        while ((value & ~0x7F) != 0) {
            out.write((value & 0x7F) | 0x80);
            value >>>= 7;
        }

        out.write(value);
    }

    public static int readVarInt(ByteBuffer buffer) {
        int value = 0;
        int position = 0;

        while (true) {
            if (!buffer.hasRemaining()) {
                throw new IllegalArgumentException("Unexpected end of buffer while reading VarInt");
            }

            int currentByte = buffer.get() & 0xFF;

            value |= (currentByte & 0x7F) << position;

            if ((currentByte & 0x80) == 0) {
                return value;
            }

            position += 7;

            if (position >= 32) {
                throw new IllegalArgumentException("VarInt is too big");
            }
        }
    }

    public static int varIntSize(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("VarInt cannot be negative: " + value);
        }

        int size = 1;

        while ((value & ~0x7F) != 0) {
            size++;
            value >>>= 7;
        }

        return size;
    }

    public static int readUnsignedByte(ByteBuffer buffer) {
        if (!buffer.hasRemaining()) {
            throw new IllegalArgumentException("Unexpected end of buffer");
        }

        return buffer.get() & 0xFF;
    }

}
