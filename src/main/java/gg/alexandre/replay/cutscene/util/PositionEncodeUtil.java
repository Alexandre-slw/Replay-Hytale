package gg.alexandre.replay.cutscene.util;

public class PositionEncodeUtil {

    private static final double POSITION_SCALE = 100.0;
    private static final double ANGLE_SCALE = 100.0;

    public static int packPosition(double value) {
        return (int) Math.round(value * POSITION_SCALE);
    }

    public static double unpackPosition(int value) {
        return value / POSITION_SCALE;
    }

    public static int packAngle(double value) {
        return (int) Math.round(value * ANGLE_SCALE);
    }

    public static double unpackAngle(int value) {
        return value / ANGLE_SCALE;
    }

}
