package gg.alexandre.replay.replay.editor.interpolation;

public class InterpolationUtil {

    public static double linear(double a, double b, double t) {
        return a + (b - a) * t;
    }

    public static double easeInOut(double a, double b, double t) {
        return a + (b - a) * (t < 0.5 ? 2 * t * t : -1 + (4 - 2 * t) * t);
    }

    public static double catmullRom(double p0, double p1, double p2, double p3, double t) {
        return 0.5 * ((2 * p1) +
                      (-p0 + p2) * t +
                      (2 * p0 - 5 * p1 + 4 * p2 - p3) * t * t +
                      (-p0 + 3 * p1 - 3 * p2 + p3) * t * t * t);
    }

}
