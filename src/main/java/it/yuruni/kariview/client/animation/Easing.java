package it.yuruni.kariview.client.animation;

public class Easing {

    public static double easeLinear(double t) {
        return t;
    }

    public static double easeInQuad(double t) {
        return t * t;
    }

    public static double easeOutQuad(double t) {
        return 1 - (1 - t) * (1 - t);
    }

    public static double easeInOutQuad(double t) {
        return t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2;
    }

    public static double easeInCubic(double t) {
        return t * t * t;
    }

    public static double easeOutCubic(double t) {
        return 1 - Math.pow(1 - t, 3);
    }

    public static double easeInOutCubic(double t) {
        return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
    }

    public static double getEasedProgress(String easingType, double progress) {
        return switch (easingType) {
            case "linear" -> easeLinear(progress);
            case "easeInQuad" -> easeInQuad(progress);
            case "easeOutQuad" -> easeOutQuad(progress);
            case "easeInOutQuad" -> easeInOutQuad(progress);
            case "easeInCubic" -> easeInCubic(progress);
            case "easeOutCubic" -> easeOutCubic(progress);
            case "easeInOutCubic" -> easeInOutCubic(progress);
            default -> easeLinear(progress);
        };
    }
}