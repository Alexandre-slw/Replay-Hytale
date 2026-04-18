package gg.alexandre.replay.ui.editor.renderers;

import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import gg.alexandre.replay.file.ReplayMetadata;
import gg.alexandre.replay.replay.state.ReplayState;

import javax.annotation.Nonnull;
import java.time.Duration;

public class TimeScaleRenderer extends BaseRenderer {

    private static final int[] STEPS_MS = {
            1, 2, 5, 10, 20, 50, 100, 200, 500,
            1000, 2000, 5000, 10000, 15000, 30000,
            60000, 120000, 300000, 600000, 900000, 1800000,
            3600000, 7200000, 21600000, 43200000,
            86400000
    };

    @Override
    public void render(@Nonnull UICommandBuilder uiCommandBuilder, @Nonnull ReplayState state, int width) {
        uiCommandBuilder.clear("#Timestamps");
        uiCommandBuilder.clear("#Ticks");

        ReplayMetadata metadata = state.file.getMetadata();
        double msPerPixel = getMsPerPixel(metadata, width);

        long totalTicks = metadata.ticks;
        long totalDurationMs = ticksToDuration(totalTicks).toMillis();

        int minorTickIntervalMs = getMinorTickInterval(msPerPixel);
        int majorTickIntervalMs = getMajorTickInterval(msPerPixel);

        if (majorTickIntervalMs <= minorTickIntervalMs) {
            majorTickIntervalMs = Math.max(1000, minorTickIntervalMs * 2);
        }

        for (long tickMs = 0; tickMs <= totalDurationMs; tickMs += minorTickIntervalMs) {
            int x = (int) Math.round(tickMs / msPerPixel);
            boolean isMajorTick = tickMs % majorTickIntervalMs == 0;

            uiCommandBuilder.appendInline("#Ticks", String.format("""
                    Group {
                      Anchor: (Left: %d, Top: %d, Height: %d, Width: 1);
                      Background: PatchStyle(TexturePath: "Assets/Mask.png", Color: #96a9be%s);
                    }
                    """, x, isMajorTick ? 0 : 4, isMajorTick ? 14 : 10, isMajorTick ? "" : "(0.4)"));

            if (isMajorTick) {
                String labelText = formatDuration(Duration.ofMillis(tickMs));
                uiCommandBuilder.appendInline("#Timestamps", String.format("""
                        Label {
                          Anchor: (Left: %d);
                          Text: "%s";
                          Style: (FontSize: 14, TextColor: #96a9be);
                        }
                        """, x + 2, labelText));
            }
        }
    }

    @Nonnull
    private String formatDuration(Duration duration) {
        long totalSeconds = duration.getSeconds();
        long hours = totalSeconds / 3600;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes % 60, seconds);
        }

        return String.format("%02d:%02d", minutes, seconds);
    }

    private int getMinorTickInterval(double msPerPixel) {
        return getInterval(msPerPixel * 15);
    }

    private int getMajorTickInterval(double msPerPixel) {
        double targetMs = msPerPixel * 80;
        targetMs = Math.max(targetMs, 1000);
        return getInterval(targetMs);
    }

    private int getInterval(double targetMs) {
        for (int s : STEPS_MS) {
            if (s >= targetMs) {
                return s;
            }
        }
        return STEPS_MS[STEPS_MS.length - 1];
    }

    private double getMsPerPixel(@Nonnull ReplayMetadata metadata, int width) {
        return ticksToDuration(metadata.ticks).toMillis() / (double) width;
    }

    @Nonnull
    private Duration ticksToDuration(long ticks) {
        return Duration.ofMillis((long) (ticks * (1000.0 / 30.0)));
    }
}