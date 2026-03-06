package it.yuruni.kariview.client.animation.states;

public record FadeState(long startTime, float startOpacity, float targetOpacity, long duration, String easingType) {}
