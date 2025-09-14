package it.yuruni.kariview.client.animation;

public class Easing {

    public static double easeInOutQuad(double t) {
        return t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2;
    }

    public static double easeLinear(double t) {
        return t;
    }

    public static double getEasedProgress(String easingType, double progress) {
        if (easingType.isEmpty()) return easeLinear(progress);
        return switch (easingType) {
            case "easeInOutQuad" -> easeInOutQuad(progress);
            default -> easeLinear(progress);
        };
    }
}