package it.yuruni.kariview.client.animation;

import com.mojang.logging.LogUtils;
import it.yuruni.kariview.client.GuiElement;
import it.yuruni.kariview.client.KariviewRenderer;
import it.yuruni.kariview.client.animation.states.*;
import it.yuruni.kariview.client.data.AnimationData;
import it.yuruni.kariview.client.data.AnimationLoader;
import it.yuruni.kariview.client.data.Keyframe;
import it.yuruni.kariview.client.data.StopAnimationData;
import it.yuruni.kariview.client.data.VariableManager;
import it.yuruni.kariview.client.data.VariableWatch;
import it.yuruni.kariview.client.data.actions.Action;
import it.yuruni.kariview.client.data.elements.GuiElementData;
import it.yuruni.kariview.client.sound.BeatDetector;
import it.yuruni.kariview.client.sound.RawAudio;
import it.yuruni.kariview.client.states.ExtendState;
import it.yuruni.kariview.client.states.PulseState;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class AnimationManager {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static AnimationData currentAnimation;
    private static long animationStartTime;
    private static final ConcurrentMap<String, GuiElement> activeElements = new ConcurrentHashMap<>();
    private static int lastKeyframeIndex = -1;

    public static final ConcurrentMap<String, SpriteState> spriteStates = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, Long> spriteUpdateIntervals = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, AnimatedStepState> animatedStepStates = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, ScaleState> scalingStates = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, FadeState> fadingStates = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, MoveState> movingStates = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, RotateState> rotatingStates = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, PulseState> pulseStates = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, ExtendState> extendStates = new ConcurrentHashMap<>();

    private static final ConcurrentMap<String, GuiElement> temporaryElements = new ConcurrentHashMap<>();

    private static boolean isWaiting = false;
    private static boolean isPlayingStopAnim = false;
    private static long stopAnimStartTime = 0;
    private static int stopKeyframeIndex = -1;
    private static final ConcurrentMap<String, String> lastWatchValues = new ConcurrentHashMap<>();
    private static String scheduledAnimNamespace = null;
    private static String scheduledAnimId = null;
    private static long scheduledAnimFireTime = -1;

    public static boolean displayTemporaryElement(String elementId, String namespace, String texturePath, double x, double y, double scale, int textureWidth, int textureHeight) {
        ResourceLocation textureResource = AssetManager.loadTexture(namespace, texturePath);
        if (textureResource != null) {
            GuiElement newElement = new GuiElement(textureResource, x, y, scale, scale, textureWidth, textureHeight);
            temporaryElements.put(elementId, newElement);
            return true;
        } else {
            return false;
        }
    }

    public static void hideAllTemporaryElements() {
        temporaryElements.clear();
    }

    public static void startAnimation(AnimationData animationData) {
        currentAnimation = animationData;
        animationStartTime = System.currentTimeMillis();
        KariviewRenderer.isGuiActive = true;
        lastKeyframeIndex = -1;
        activeElements.clear();
        spriteStates.clear();
        spriteUpdateIntervals.clear();
        rotatingStates.clear();
        pulseStates.clear();
        BeatDetector.registeredAudioElements.clear();
        isWaiting = false;
        isPlayingStopAnim = false;
        stopKeyframeIndex = -1;
        lastWatchValues.clear();
        scheduledAnimFireTime = -1;
    }

    public static void scheduleAnimation(String namespace, String animId, long delayMs) {
        scheduledAnimNamespace = namespace;
        scheduledAnimId = animId;
        scheduledAnimFireTime = System.currentTimeMillis() + delayMs;
    }

    public static void triggerStop() {
        if (currentAnimation == null) return;
        StopAnimationData stopAnim = currentAnimation.getStopAnimation();
        if (stopAnim != null && stopAnim.getKeyframes() != null && !stopAnim.getKeyframes().isEmpty()) {
            isWaiting = false;
            isPlayingStopAnim = true;
            stopAnimStartTime = System.currentTimeMillis();
            stopKeyframeIndex = -1;
        } else {
            stopAllAnimations();
        }
    }

    public static void playAnimation(String namespace, String animationId) {
        AnimationData data = AnimationLoader.loadAnimation(namespace, animationId);
        if (data != null) {
            startAnimation(data);
        }
    }

    public static void tick() {
        if (currentAnimation == null) {
            return;
        }

        long elapsed = System.currentTimeMillis() - animationStartTime;

        animatedStepStates.entrySet().removeIf(entry -> {
            String elementId = entry.getKey();
            AnimatedStepState state = entry.getValue();
            GuiElement activeElement = activeElements.get(elementId);

            if (activeElement == null) {
                return true;
            }

            long elapsedSinceStart = elapsed - state.startTime();
            if (elapsedSinceStart >= state.duration()) {
                int finalIndex = state.shouldLoop() ? (state.targetSpriteIndex() + state.totalSprites()) % state.totalSprites() : state.targetSpriteIndex();
                activeElement.setTexture(spriteStates.get(elementId).getSprites().get(finalIndex));
                spriteStates.get(elementId).setCurrentIndex(finalIndex);
                return true;
            }

            int newIndex = getNewIndex(state, (double) elapsedSinceStart);

            spriteStates.get(elementId).setCurrentIndex(newIndex);
            activeElement.setTexture(spriteStates.get(elementId).getCurrentSprite());

            return false;
        });

        for (Map.Entry<String, PulseState> entry : pulseStates.entrySet()) {
            String elementId = entry.getKey();
            PulseState state = entry.getValue();
            GuiElement element = activeElements.get(elementId);
            if (element != null) {
                long pulseDuration = System.currentTimeMillis() - state.lastPulseTime;

                if (state.isPulsing) {
                    double progress = Math.min(1.0, (double) pulseDuration / 50.0);
                    double easedProgress = Easing.getEasedProgress(state.easingType, progress);
                    double newScale = state.startScale + (state.targetScale - state.startScale) * easedProgress;
                    element.setXScale(newScale);
                    element.setYScale(newScale);
                    if (progress >= 1.0) {
                        state.isPulsing = false;
                        state.startScale = element.getXScale();
                        state.lastPulseTime = System.currentTimeMillis();
                    }
                } else {
                    double progress = Math.min(1.0, (double) pulseDuration / state.decay);
                    double easedProgress = Easing.getEasedProgress(state.easingType, progress);
                    double newScale = state.startScale - (state.startScale - state.defaultValue) * easedProgress;
                    element.setXScale(newScale);
                    element.setYScale(newScale);
                }
            }
        }

        for (Map.Entry<String, ExtendState> entry : extendStates.entrySet()) {
            String elementId = entry.getKey();
            ExtendState state = entry.getValue();
            GuiElement element = activeElements.get(elementId);
            if (element == null) continue;

            long elapsedSinceBeat = System.currentTimeMillis() - state.lastBeatTime;

            if (state.isExtending) {
                double progress = Math.min(1.0, (double) elapsedSinceBeat / state.extendTime);
                double easedProgress = Easing.getEasedProgress(state.easing, progress);
                double currentValue;

                if (state.direction.equalsIgnoreCase("left") || state.direction.equalsIgnoreCase("right")) {
                    currentValue = state.startValue + (state.targetValue - state.startValue) * easedProgress;
                    element.setXScale(currentValue);
                } else {
                    currentValue = state.startValue + (state.targetValue - state.startValue) * easedProgress;
                    element.setYScale(currentValue);
                }

                if (progress >= 1.0) {
                    state.isExtending = false;
                    state.startValue = currentValue;
                    state.lastBeatTime = System.currentTimeMillis();
                }
            } else {
                double progress = Math.min(1.0, (double) elapsedSinceBeat / state.decay);
                double easedProgress = Easing.getEasedProgress(state.easing, progress);
                double currentValue;

                if (state.direction.equalsIgnoreCase("left") || state.direction.equalsIgnoreCase("right")) {
                    currentValue = state.startValue - (state.startValue - state.defaultValue) * easedProgress;
                    element.setXScale(currentValue);
                } else {
                    currentValue = state.startValue - (state.startValue - state.defaultValue) * easedProgress;
                    element.setYScale(currentValue);
                }

                if (progress >= 1.0) {
                    extendStates.remove(elementId);
                }
            }
        }

        scalingStates.entrySet().removeIf(entry -> {
            String elementId = entry.getKey();
            ScaleState state = entry.getValue();
            GuiElement element = activeElements.get(elementId);
            if (element == null) {
                return true;
            }

            long elapsedSinceStart = (System.currentTimeMillis() - animationStartTime) - state.startTime();
            if (elapsedSinceStart >= state.duration()) {
                element.setXScale(state.targetXScale());
                element.setYScale(state.targetYScale());
                if (state.direction() != null) {
                    switch (state.direction()) {
                        case "LEFT":
                            element.setX(state.startX() + (state.startXScale() - state.targetXScale()) * element.getWidth());
                            break;
                        case "UP":
                            element.setY(state.startY() + (state.startYScale() - state.targetYScale()) * element.getHeight());
                            break;
                    }
                }
                return true;
            }

            double progress = (double) elapsedSinceStart / state.duration();
            double easedProgress = Easing.getEasedProgress(state.easingType(), progress);
            double newXScale = state.startXScale() + (state.targetXScale() - state.startXScale()) * easedProgress;
            double newYScale = state.startYScale() + (state.targetYScale() - state.startYScale()) * easedProgress;

            element.setXScale(newXScale);
            element.setYScale(newYScale);

            if (state.direction() != null) {
                switch (state.direction()) {
                    case "LEFT":
                        element.setX(state.startX() + (state.startXScale() - newXScale) * element.getWidth());
                        break;
                    case "UP":
                        element.setY(state.startY() + (state.startYScale() - newYScale) * element.getHeight());
                        break;
                }
            }
            return false;
        });

        fadingStates.entrySet().removeIf(entry -> {
            String elementId = entry.getKey();
            FadeState state = entry.getValue();
            GuiElement element = activeElements.get(elementId);
            if (element == null) {
                return true;
            }

            long elapsedSinceStart = (System.currentTimeMillis() - animationStartTime) - state.startTime();
            if (elapsedSinceStart >= state.duration()) {
                element.setOpacity(state.targetOpacity());
                return true;
            }

            double progress = (double) elapsedSinceStart / state.duration();
            double easedProgress = Easing.getEasedProgress(state.easingType(), progress);
            float newOpacity = (float) (state.startOpacity() + (state.targetOpacity() - state.startOpacity()) * easedProgress);
            element.setOpacity(newOpacity);

            return false;
        });

        movingStates.entrySet().removeIf(entry -> {
            String elementId = entry.getKey();
            MoveState state = entry.getValue();
            GuiElement element = activeElements.get(elementId);
            if (element == null) {
                return true;
            }

            long elapsedSinceStart = (System.currentTimeMillis() - animationStartTime) - state.startTime();
            if (elapsedSinceStart >= state.duration()) {
                element.setX(state.targetX());
                element.setY(state.targetY());
                return true;
            }

            double progress = (double) elapsedSinceStart / state.duration();
            double easedProgress = Easing.getEasedProgress(state.easingType(), progress);

            double newX = state.startX() + (state.targetX() - state.startX()) * easedProgress;
            double newY = state.startY() + (state.targetY() - state.startY()) * easedProgress;

            element.setX(newX);
            element.setY(newY);

            return false;
        });

        rotatingStates.entrySet().removeIf(entry -> {
            String elementId = entry.getKey();
            RotateState state = entry.getValue();
            GuiElement element = activeElements.get(elementId);
            if (element == null) {
                return true;
            }

            long elapsedSinceStart = (System.currentTimeMillis() - animationStartTime) - state.startTime();
            if (elapsedSinceStart >= state.duration()) {
                element.setAngle(state.targetAngle());
                return true;
            }

            double progress = (double) elapsedSinceStart / state.duration();
            double easedProgress = Easing.getEasedProgress(state.easingType(), progress);

            double newAngle = state.startAngle() + (state.targetAngle() - state.startAngle()) * easedProgress;
            element.setAngle(newAngle);

            return false;
        });

        for (Map.Entry<String, Long> entry : spriteUpdateIntervals.entrySet()) {
            String elementId = entry.getKey();
            long updateInterval = entry.getValue();

            SpriteState spriteState = spriteStates.get(elementId);
            GuiElement activeElement = activeElements.get(elementId);

            if (spriteState != null && activeElement != null && elapsed - spriteState.getLastUpdateTime() >= updateInterval) {
                int nextIndex = spriteState.getCurrentIndex() + 1;
                if (nextIndex >= spriteState.getSprites().size()) {
                    nextIndex = 0;
                }
                spriteState.setCurrentIndex(nextIndex);
                spriteState.setLastUpdateTime(elapsed);

                activeElement.setTexture(spriteState.getCurrentSprite());
            }
        }

        List<Keyframe> keyframes = currentAnimation.getKeyframes();
        for (int i = lastKeyframeIndex + 1; i < keyframes.size(); i++) {
            Keyframe keyframe = keyframes.get(i);
            if (elapsed >= keyframe.getTimestamp()) {
                executeKeyframeActions(keyframe.getActions());
                lastKeyframeIndex = i;
            } else {
                break;
            }
        }

        if (scheduledAnimFireTime > 0 && System.currentTimeMillis() >= scheduledAnimFireTime) {
            scheduledAnimFireTime = -1;
            playAnimation(scheduledAnimNamespace, scheduledAnimId);
            return;
        }

        if (isPlayingStopAnim) {
            StopAnimationData stopAnim = currentAnimation.getStopAnimation();
            long stopElapsed = System.currentTimeMillis() - stopAnimStartTime;
            List<Keyframe> stopKeyframes = stopAnim.getKeyframes();
            for (int i = stopKeyframeIndex + 1; i < stopKeyframes.size(); i++) {
                Keyframe kf = stopKeyframes.get(i);
                if (stopElapsed >= kf.getTimestamp()) {
                    executeKeyframeActions(kf.getActions());
                    stopKeyframeIndex = i;
                } else {
                    break;
                }
            }
            if (stopKeyframeIndex >= stopKeyframes.size() - 1) {
                stopAllAnimations();
            }
            return;
        }

        if (currentAnimation.getVariableWatches() != null) {
            for (VariableWatch watch : currentAnimation.getVariableWatches()) {
                String watchKey = watch.getNamespace() + ":" + watch.getVariable();
                String currentVal = VariableManager.get(watch.getNamespace(), watch.getVariable());
                String lastVal = lastWatchValues.get(watchKey);
                if (!java.util.Objects.equals(currentVal, lastVal)) {
                    lastWatchValues.put(watchKey, currentVal);
                    if (watch.getActions() != null) {
                        executeKeyframeActions(watch.getActions());
                    }
                }
            }
        }

        if (!isWaiting) {
            if (lastKeyframeIndex >= keyframes.size() - 1) {
                if ("wait".equals(currentAnimation.getEndAction())) {
                    isWaiting = true;
                } else {
                    stopAllAnimations();
                }
            }
        }
    }

    private static int getNewIndex(AnimatedStepState state, double elapsedSinceStart) {
        int totalSteps = state.totalSteps();
        int currentStep = (int) (elapsedSinceStart / state.duration() * totalSteps);

        int direction = state.targetSpriteIndex() >= state.startSpriteIndex() ? 1 : -1;
        if (state.shouldLoop() && state.targetSpriteIndex() < state.startSpriteIndex()) {
            direction = 1;
        }

        int newIndex;
        if (state.shouldLoop()) {
            newIndex = (state.startSpriteIndex() + currentStep) % state.totalSprites();
        } else {
            newIndex = state.startSpriteIndex() + (currentStep * direction);
            if ((direction > 0 && newIndex > state.targetSpriteIndex()) || (direction < 0 && newIndex < state.targetSpriteIndex())) {
                newIndex = state.targetSpriteIndex();
            }
        }
        return newIndex;
    }

    public static void stopAllAnimations() {
        KariviewRenderer.isGuiActive = false;
        currentAnimation = null;
        activeElements.clear();
        lastKeyframeIndex = -1;
        RawAudio.stopAll();
        isWaiting = false;
        isPlayingStopAnim = false;
        scheduledAnimFireTime = -1;
        lastWatchValues.clear();
    }

    public static void reload() {
        currentAnimation = null;
        animationStartTime = 0;
        lastKeyframeIndex = -1;
        activeElements.clear();
        spriteStates.clear();
        animatedStepStates.clear();
        scalingStates.clear();
        fadingStates.clear();
        movingStates.clear();
        spriteUpdateIntervals.clear();

        AnimationLoader.loadAllAnimations();
    }

    public static void triggerPulse(String elementId, double targetScale, double decay, double defaultValue, String easingType) {
        if (activeElements.containsKey(elementId)) {
            PulseState state = pulseStates.get(elementId);
            if (state == null) {
                state = new PulseState(targetScale, decay, defaultValue, easingType);
                pulseStates.put(elementId, state);
            }
            state.lastPulseTime = System.currentTimeMillis();
            state.startScale = activeElements.get(elementId).getXScale();
            state.targetScale = Math.max(state.startScale, targetScale);
            state.isPulsing = true;
        }
    }

    public static void triggerSpriteChange(String elementId, int steps, int duration, boolean shouldLoop) {
        if (activeElements.containsKey(elementId)) {
            SpriteState spriteState = spriteStates.get(elementId);
            GuiElement activeElement = activeElements.get(elementId);
            if (spriteState != null && activeElement != null) {
                int currentSpriteIndex = spriteState.getCurrentIndex();
                int targetIndex = currentSpriteIndex + steps;

                if (!shouldLoop && (targetIndex < 0 || targetIndex >= spriteState.getSprites().size())) {
                    LOGGER.warn("StepChange (audio element): Stepping out of bounds for non-looping sprite.");
                    return;
                }

                animatedStepStates.put(elementId, new AnimatedStepState(
                        System.currentTimeMillis() - animationStartTime,
                        currentSpriteIndex,
                        targetIndex,
                        duration,
                        shouldLoop,
                        spriteState.getSprites().size(),
                        steps
                ));
            } else {
                LOGGER.error("StepChange (audio element): No sprite animation state or active element found for: {}", elementId);
            }
        }
    }

    public static void triggerExtend(String elementId, String direction, double targetValue, double decay, double defaultValue, double extendTime, String easing) {
        if (activeElements.containsKey(elementId)) {
            GuiElement element = activeElements.get(elementId);
            ExtendState state = extendStates.get(elementId);
            if (state == null) {
                state = new ExtendState(direction, targetValue, decay, defaultValue, extendTime, easing);
                extendStates.put(elementId, state);
            }
            state.lastBeatTime = System.currentTimeMillis();
            state.targetValue = targetValue;
            state.startValue = direction.equalsIgnoreCase("left") || direction.equalsIgnoreCase("right") ? element.getXScale() : element.getYScale();
            state.isExtending = true;
        }
    }

    private static void executeKeyframeActions(List<Action> actions) {
        if (actions == null) return;
        AnimationContext ctx = new AnimationContext(
                currentAnimation, animationStartTime,
                activeElements, spriteStates, spriteUpdateIntervals,
                animatedStepStates, scalingStates, fadingStates, movingStates, rotatingStates
        );
        for (Action action : actions) {
            action.execute(ctx);
        }
    }

    public static Optional<ResourceLocation> getTextureLocation(String elementId) {
        GuiElement element = activeElements.get(elementId);
        if (element == null) {
            return Optional.empty();
        }

        GuiElementData elementData = currentAnimation.getElementById(elementId);

        if (elementData.getTexture() != null) {
            return Optional.ofNullable(AssetManager.loadTexture(currentAnimation.getNamespace(), elementData.getTexture()));
        } else if (elementData.getTexturePathPattern() != null) {
            SpriteState spriteState = spriteStates.get(elementId);
            if (spriteState != null) {
                return Optional.ofNullable(spriteState.getCurrentSprite());
            } else {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

    public static Map<String, GuiElement> getActiveElements() {
        Map<String, GuiElement> allElements = new HashMap<>();
        allElements.putAll(activeElements);
        allElements.putAll(temporaryElements);
        return new TreeMap<>(allElements);
    }
}
