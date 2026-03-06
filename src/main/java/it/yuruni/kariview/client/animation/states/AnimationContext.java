package it.yuruni.kariview.client.animation.states;

import it.yuruni.kariview.client.GuiElement;
import it.yuruni.kariview.client.animation.SpriteState;
import it.yuruni.kariview.client.data.AnimationData;

import java.util.concurrent.ConcurrentMap;

public class AnimationContext {
    public final AnimationData currentAnimation;
    public final long animationStartTime;
    public final ConcurrentMap<String, GuiElement> activeElements;
    public final ConcurrentMap<String, SpriteState> spriteStates;
    public final ConcurrentMap<String, Long> spriteUpdateIntervals;
    public final ConcurrentMap<String, AnimatedStepState> animatedStepStates;
    public final ConcurrentMap<String, ScaleState> scalingStates;
    public final ConcurrentMap<String, FadeState> fadingStates;
    public final ConcurrentMap<String, MoveState> movingStates;
    public final ConcurrentMap<String, RotateState> rotatingStates;

    public AnimationContext(
            AnimationData currentAnimation,
            long animationStartTime,
            ConcurrentMap<String, GuiElement> activeElements,
            ConcurrentMap<String, SpriteState> spriteStates,
            ConcurrentMap<String, Long> spriteUpdateIntervals,
            ConcurrentMap<String, AnimatedStepState> animatedStepStates,
            ConcurrentMap<String, ScaleState> scalingStates,
            ConcurrentMap<String, FadeState> fadingStates,
            ConcurrentMap<String, MoveState> movingStates,
            ConcurrentMap<String, RotateState> rotatingStates) {
        this.currentAnimation = currentAnimation;
        this.animationStartTime = animationStartTime;
        this.activeElements = activeElements;
        this.spriteStates = spriteStates;
        this.spriteUpdateIntervals = spriteUpdateIntervals;
        this.animatedStepStates = animatedStepStates;
        this.scalingStates = scalingStates;
        this.fadingStates = fadingStates;
        this.movingStates = movingStates;
        this.rotatingStates = rotatingStates;
    }
}
