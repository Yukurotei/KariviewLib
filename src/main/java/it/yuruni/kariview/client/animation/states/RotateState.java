package it.yuruni.kariview.client.animation.states;

public record RotateState(long startTime, double startAngle, double targetAngle, long duration, String easingType) {}
