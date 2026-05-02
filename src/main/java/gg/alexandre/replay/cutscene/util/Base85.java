package gg.alexandre.replay.cutscene.util;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class Base85 {

    private static final char[] BASE85_ALPHABET =
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ.-:+=^!/*?&<>()[]{}@%$#".toCharArray();

    private static final int[] BASE85_REVERSE = new int[128];

    static {
        Arrays.fill(BASE85_REVERSE, -1);

        for (int i = 0; i < BASE85_ALPHABET.length; i++) {
            BASE85_REVERSE[BASE85_ALPHABET[i]] = i;
        }
    }

    @Nonnull
    public static String encodeBase85(@Nonnull byte[] data) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        ByteBufferUtil.writeVarInt(bytes, data.length);
        bytes.writeBytes(data);

        byte[] input = bytes.toByteArray();

        StringBuilder output = new StringBuilder(((input.length + 3) / 4) * 5);

        for (int i = 0; i < input.length; i += 4) {
            long value = 0;

            for (int j = 0; j < 4; j++) {
                value <<= 8;

                int index = i + j;

                if (index < input.length) {
                    value |= input[index] & 0xFFL;
                }
            }

            char[] block = new char[5];

            for (int j = 4; j >= 0; j--) {
                block[j] = BASE85_ALPHABET[(int) (value % 85)];
                value /= 85;
            }

            output.append(block);
        }

        return output.toString();
    }

    @Nonnull
    public static byte[] decodeBase85(@Nonnull String data) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        for (int i = 0; i < data.length(); i += 5) {
            long value = 0;

            for (int j = 0; j < 5; j++) {
                int index = i + j;

                int digit = 84;

                if (index < data.length()) {
                    char c = data.charAt(index);

                    if (c >= BASE85_REVERSE.length || BASE85_REVERSE[c] == -1) {
                        throw new IllegalArgumentException("Invalid Base85 character: " + c);
                    }

                    digit = BASE85_REVERSE[c];
                }

                value = value * 85 + digit;
            }

            output.write((int) ((value >>> 24) & 0xFF));
            output.write((int) ((value >>> 16) & 0xFF));
            output.write((int) ((value >>> 8) & 0xFF));
            output.write((int) (value & 0xFF));
        }

        byte[] decoded = output.toByteArray();
        ByteBuffer buffer = ByteBuffer.wrap(decoded);

        int length = ByteBufferUtil.readVarInt(buffer);

        if (length < 0 || length > buffer.remaining()) {
            throw new IllegalArgumentException("Invalid Base85 payload length: " + length);
        }

        byte[] result = new byte[length];
        buffer.get(result);

        return result;
    }

}
