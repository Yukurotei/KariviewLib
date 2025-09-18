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

    public static double easeInQuart(double t) {
        return t * t * t * t;
    }

    public static double easeOutQuart(double t) {
        return 1 - Math.pow(1 - t, 4);
    }

    public static double easeInOutQuart(double t) {
        return t < 0.5 ? 8 * t * t * t * t : 1 - Math.pow(-2 * t + 2, 4) / 2;
    }

    public static double easeInQuint(double t) {
        return t * t * t * t * t;
    }

    public static double easeOutQuint(double t) {
        return 1 - Math.pow(1 - t, 5);
    }

    public static double easeInOutQuint(double t) {
        return t < 0.5 ? 16 * t * t * t * t * t : 1 - Math.pow(-2 * t + 2, 5) / 2;
    }

    public static double easeInSine(double t) {
        return 1 - Math.cos((t * Math.PI) / 2);
    }

    public static double easeOutSine(double t) {
        return Math.sin((t * Math.PI) / 2);
    }

    public static double easeInOutSine(double t) {
        return -(Math.cos(Math.PI * t) - 1) / 2;
    }

    public static double easeInExpo(double t) {
        return t == 0 ? 0 : Math.pow(2, 10 * (t - 1));
    }

    public static double easeOutExpo(double t) {
        return t == 1 ? 1 : 1 - Math.pow(2, -10 * t);
    }

    public static double easeInOutExpo(double t) {
        return t == 0 ? 0 : t == 1 ? 1 : t < 0.5 ? Math.pow(2, 20 * t - 10) / 2 : (2 - Math.pow(2, -20 * t + 10)) / 2;
    }

    public static double easeInCirc(double t) {
        return 1 - Math.sqrt(1 - Math.pow(t, 2));
    }

    public static double easeOutCirc(double t) {
        return Math.sqrt(1 - Math.pow(t - 1, 2));
    }

    public static double easeInOutCirc(double t) {
        return t < 0.5 ? (1 - Math.sqrt(1 - Math.pow(2 * t, 2))) / 2 : (Math.sqrt(1 - Math.pow(-2 * t + 2, 2)) + 1) / 2;
    }

    public static double easeInBack(double t) {
        final double s = 1.70158;
        return t * t * ((s + 1) * t - s);
    }

    public static double easeOutBack(double t) {
        final double s = 1.70158;
        return 1 + (t - 1) * (t - 1) * ((s + 1) * (t - 1) + s);
    }

    public static double easeInOutBack(double t) {
        final double s = 1.70158;
        return t < 0.5 ? Math.pow(2 * t, 2) * ((s + 1) * 2 * t - s) / 2 : Math.pow(2 * t - 2, 2) * ((s + 1) * (t * 2 - 2) + s) / 2 + 1;
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
            case "easeInQuart" -> easeInQuart(progress);
            case "easeOutQuart" -> easeOutQuart(progress);
            case "easeInOutQuart" -> easeInOutQuart(progress);
            case "easeInQuint" -> easeInQuint(progress);
            case "easeOutQuint" -> easeOutQuint(progress);
            case "easeInOutQuint" -> easeInOutQuint(progress);
            case "easeInSine" -> easeInSine(progress);
            case "easeOutSine" -> easeOutSine(progress);
            case "easeInOutSine" -> easeInOutSine(progress);
            case "easeInExpo" -> easeInExpo(progress);
            case "easeOutExpo" -> easeOutExpo(progress);
            case "easeInOutExpo" -> easeInOutExpo(progress);
            case "easeInCirc" -> easeInCirc(progress);
            case "easeOutCirc" -> easeOutCirc(progress);
            case "easeInOutCirc" -> easeInOutCirc(progress);
            case "easeInBack" -> easeInBack(progress);
            case "easeOutBack" -> easeOutBack(progress);
            case "easeInOutBack" -> easeInOutBack(progress);
            default -> easeLinear(progress);
        };
    }
}