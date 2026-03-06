package it.yuruni.kariview.client.animation.states;

public record AnimatedStepState(long startTime, int startSpriteIndex, int targetSpriteIndex, long duration, boolean loop, int totalSprites, int totalSteps) {
    public boolean shouldLoop() {
        return loop;
    }
}
