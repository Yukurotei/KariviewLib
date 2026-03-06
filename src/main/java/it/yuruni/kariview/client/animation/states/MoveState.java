package it.yuruni.kariview.client.animation.states;

public record MoveState(long startTime, double startX, double startY, double targetX, double targetY, long duration, String easingType) {}
