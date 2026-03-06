package it.yuruni.kariview.client.animation.states;

public record ScaleState(long startTime, double startXScale, double startYScale, double targetXScale, double targetYScale, long duration, String direction, double startX, double startY, String easingType) {}
