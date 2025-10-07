package it.yuruni.kariview.client.animation;

import com.mojang.logging.LogUtils;
import it.yuruni.kariview.client.GuiElement;
import it.yuruni.kariview.client.KariviewRenderer;
import it.yuruni.kariview.client.data.AnimationData;
import it.yuruni.kariview.client.data.AnimationLoader;
import it.yuruni.kariview.client.data.Keyframe;
import it.yuruni.kariview.client.data.actions.*;
import it.yuruni.kariview.client.data.elements.GuiElementData;
import it.yuruni.kariview.client.sound.BeatDetector;
import it.yuruni.kariview.client.sound.RawAudio;
import it.yuruni.kariview.client.states.ExtendState;
import it.yuruni.kariview.client.states.PulseState;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.io.File;
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

    private record AnimatedStepState(long startTime, int startSpriteIndex, int targetSpriteIndex, long duration,
                                     boolean loop, int totalSprites, int totalSteps) {
        public boolean shouldLoop() {
            return loop;
        }
    }

    private record ScaleState(long startTime, double startXScale, double startYScale, double targetXScale, double targetYScale, long duration, String direction, double startX, double startY, String easingType) {}

    private record FadeState(long startTime, float startOpacity, float targetOpacity, long duration, String easingType) {}

    private record MoveState(long startTime, double startX, double startY, double targetX, double targetY, long duration, String easingType) {}

    private record RotateState(long startTime, double startAngle, double targetAngle, long duration, String easingType) {}

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
    }

    public static void playAnimation(String namespace, String animationId) {
        AnimationData data = AnimationLoader.loadAnimation(namespace, animationId);
        if (data != null) {
            //LOGGER.info("Starting animation: {}", data.getId());
            startAnimation(data);
        } else {
            //LOGGER.error("Failed to load animation: {}:{}", namespace, animationId);
        }
    }

    public static void tick() {
        if (currentAnimation == null) {
            return;
        }

        long elapsed = System.currentTimeMillis() - animationStartTime;

        //Update step elements
        animatedStepStates.entrySet().removeIf(entry -> {
            String elementId = entry.getKey();
            AnimatedStepState state = entry.getValue();
            GuiElement activeElement = activeElements.get(elementId);

            if (activeElement == null) {
                return true; // Remove if element is no longer active
            }

            long elapsedSinceStart = elapsed - state.startTime();
            if (elapsedSinceStart >= state.duration()) {
                // Animation is complete, set to final frame and remove state
                int finalIndex = state.shouldLoop() ? (state.targetSpriteIndex() + state.totalSprites()) % state.totalSprites() : state.targetSpriteIndex();
                activeElement.setTexture(spriteStates.get(elementId).getSprites().get(finalIndex));
                spriteStates.get(elementId).setCurrentIndex(finalIndex);
                return true; // Remove the state
            }

            // Calculate the current frame based on elapsed time
            int newIndex = getNewIndex(state, (double) elapsedSinceStart);

            spriteStates.get(elementId).setCurrentIndex(newIndex);
            activeElement.setTexture(spriteStates.get(elementId).getCurrentSprite());

            return false;
        });

        //Update audio driven pulse states
        for (Map.Entry<String, PulseState> entry : pulseStates.entrySet()) {
            String elementId = entry.getKey();
            PulseState state = entry.getValue();
            GuiElement element = activeElements.get(elementId);
            if (element != null) {
                long pulseDuration = System.currentTimeMillis() - state.lastPulseTime;

                if (state.isPulsing) {
                    // Pulse Up: animate from the start scale to the new target
                    double progress = Math.min(1.0, (double) pulseDuration / 50.0); // 50ms pulse up
                    double easedProgress = Easing.getEasedProgress(state.easingType, progress);
                    double newScale = state.startScale + (state.targetScale - state.startScale) * easedProgress;
                    element.setXScale(newScale);
                    element.setYScale(newScale);
                    if (progress >= 1.0) {
                        state.isPulsing = false;
                        state.startScale = element.getXScale(); // Set the start scale for decay
                        state.lastPulseTime = System.currentTimeMillis();
                    }
                } else {
                    // Decay: animate from the current scale back down to default
                    double progress = Math.min(1.0, (double) pulseDuration / state.decay);
                    double easedProgress = Easing.getEasedProgress(state.easingType, progress);
                    double newScale = state.startScale - (state.startScale - state.defaultValue) * easedProgress;
                    element.setXScale(newScale);
                    element.setYScale(newScale);
                }
            }
        }

        //Update audio driven extend states
        for (Map.Entry<String, ExtendState> entry : extendStates.entrySet()) {
            String elementId = entry.getKey();
            ExtendState state = entry.getValue();
            GuiElement element = activeElements.get(elementId);
            if (element == null) continue;

            long elapsedSinceBeat = System.currentTimeMillis() - state.lastBeatTime;

            if (state.isExtending) {
                // Extension phase
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
                    state.startValue = currentValue; // Set the start value for the decay
                    state.lastBeatTime = System.currentTimeMillis();
                }
            } else {
                // Decay phase
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
                    extendStates.remove(elementId); // Remove state when decay is complete
                }
            }
        }

        //Update scaling elements AND extending elements
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
                //Set position to its final resting place
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

            //Adjust position based on direction
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

        //Update fading elements
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

        //Update moving elements
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

        //Update rotating elements
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

        // Loop through all active sprite animations and update them
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

        // Check if the animation should end
        if (lastKeyframeIndex >= keyframes.size() - 1) {
            stopAllAnimations();
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
        if (actions == null) {
            return;
        }

        for (Action action : actions) {
            //LOGGER.info(action.toString());
            if (action instanceof ShowElementAction) {
                handleShowElement((ShowElementAction) action);
                /*TO CALCULATE WIDTH AND HEIGHT FROM ACTUAL NUMBER TO PERCENTAGE
                    - Find out height to width ratio (height / width)
                    - width: percentage of the screen you want to take up (e.g. 50% is 0.5, on and on)
                    - height: multiply the width value by the ratio
                 */
            } else if (action instanceof HideElementAction) {
                handleHideElement((HideElementAction) action);
            } else if (action instanceof PlaySoundAction) {
                handlePlaySound((PlaySoundAction) action);
            } else if (action instanceof StopAllSoundAction) {
                RawAudio.stopAll();
            } else if (action instanceof UpdateSpriteAction) {
                handleUpdateSprite((UpdateSpriteAction) action);
            } else if (action instanceof SetSpriteIndexAction) {
                handleSetSpriteIndex((SetSpriteIndexAction) action);
            } else if (action instanceof StepSpriteIndexAction) {
                handleStepSpriteIndex((StepSpriteIndexAction) action);
            } else if (action instanceof StopSpriteAnimationAction) {
                handleStopSpriteAnimation((StopSpriteAnimationAction) action);
            } else if (action instanceof ScaleAction) {
                handleScaleAction((ScaleAction) action);
            } else if (action instanceof MoveAction) {
                handleMoveAction((MoveAction) action);
            } else if (action instanceof RotateAction) {
                handleRotateAction((RotateAction) action);
            } else if (action instanceof ExtendAction) {
                handleExtendAction((ExtendAction) action);
            } else if (action instanceof RegisterAudioElementAction) {
                handleRegisterAudioElementAction((RegisterAudioElementAction) action);
            } else if (action instanceof UnregisterAudioElementAction) {
                handleUnregisterAudioElementAction((UnregisterAudioElementAction) action);
            }
        }
    }

    private static void handleRegisterAudioElementAction(RegisterAudioElementAction action) {
        if (activeElements.containsKey(action.getElementId())) {
            BeatDetector.registeredAudioElements.put(action.getElementId(), action);
        } else {
            LOGGER.error("Cannot register audio element. Element not found: {}", action.getElementId());
        }
    }

    private static void handleUnregisterAudioElementAction(UnregisterAudioElementAction action) {
        BeatDetector.registeredAudioElements.remove(action.getElementId());
    }


    private static void handleExtendAction(ExtendAction action) {
        GuiElement element = activeElements.get(action.getElementId());
        if (element != null) {
            double targetX = element.getXScale();
            double targetY = element.getYScale();

            switch (action.getDirection()) {
                case "LEFT":
                case "RIGHT":
                    targetX = action.getTargetValue();
                    break;
                case "UP":
                case "DOWN":
                    targetY = action.getTargetValue();
                    break;
            }

            scalingStates.put(action.getElementId(), new ScaleState(
                    System.currentTimeMillis() - animationStartTime,
                    element.getXScale(),
                    element.getYScale(),
                    targetX,
                    targetY,
                    action.getDuration(),
                    action.getDirection(),
                    element.getX(),
                    element.getY(),
                    action.getEasingType()
            ));
        } else {
            LOGGER.error("ExtendAction: No active element found for id: {}", action.getElementId());
        }
    }

    private static void handleRotateAction(RotateAction action) {
        GuiElement element = activeElements.get(action.getElementId());
        if (element != null) {
            rotatingStates.put(action.getElementId(), new RotateState(
                    System.currentTimeMillis() - animationStartTime,
                    element.getAngle(),
                    action.getTargetAngle(),
                    action.getDuration(),
                    action.getEasingType()
            ));
        } else {
            LOGGER.error("RotateAction: No active element found for id: {}", action.getElementId());
        }
    }

    private static void handleMoveAction(MoveAction action) {
        GuiElement element = activeElements.get(action.getElementId());
        if (element != null) {
            movingStates.put(action.getElementId(), new MoveState(
                    System.currentTimeMillis() - animationStartTime,
                    element.getX(),
                    element.getY(),
                    action.getTargetX(),
                    action.getTargetY(),
                    action.getDuration(),
                    action.getEasingType()
            ));
        } else {
            LOGGER.error("MoveAction: No active element found for id: {}", action.getElementId());
        }
    }

    private static void handleScaleAction(ScaleAction action) {
        GuiElement element = activeElements.get(action.getElementId());
        if (element != null) {
            scalingStates.put(action.getElementId(), new ScaleState(
                    System.currentTimeMillis() - animationStartTime,
                    element.getXScale(),
                    element.getYScale(),
                    element.getXScale() < action.getTargetScale() ? action.getTargetScale() / 2 : action.getTargetScale(),
                    element.getYScale() < action.getTargetScale() ? action.getTargetScale() / 2 : action.getTargetScale(),
                    action.getDuration(),
                    "ALL",
                    element.getX(),
                    element.getY(),
                    action.getEasingType()
            ));
        } else {
            LOGGER.error("ScaleAction: No active element found for id: {}", action.getElementId());
        }
    }

    private static void handleUpdateSprite(UpdateSpriteAction action) {
        GuiElementData elementData = currentAnimation.getElementById(action.getElementId());
        if (elementData != null && elementData.getTexturePathPattern() != null) {
            String elementId = elementData.getId();
            List<ResourceLocation> sprites = SpriteManager.loadSprites(currentAnimation.getNamespace(), elementData.getTexturePathPattern());
            if (!sprites.isEmpty()) {
                spriteStates.put(elementId, new SpriteState(sprites));
                spriteUpdateIntervals.put(elementId, action.getUpdateInterval());
            } else {
                LOGGER.error("UpdateSpriteAction: No sprites found for pattern: " + elementData.getTexturePathPattern());
            }
        } else {
            LOGGER.error("UpdateSpriteAction: Element with ID '" + action.getElementId() + "' not found or has no texture_path_pattern.");
        }
    }

    private static void handleSetSpriteIndex(SetSpriteIndexAction action) {
        SpriteState spriteState = spriteStates.get(action.getElementId());
        GuiElement activeElement = activeElements.get(action.getElementId());
        if (spriteState != null && activeElement != null) {
            int newIndex = action.getTargetIndex();
            if (!action.shouldLoop() && (newIndex < 0 || newIndex >= spriteState.getSprites().size())) {
                LOGGER.warn("SetSpriteIndexAction: Target index " + newIndex + " is out of bounds for non-looping sprite.");
                return;
            }
            spriteState.setCurrentIndex(newIndex % spriteState.getSprites().size());
            activeElement.setTexture(spriteState.getCurrentSprite());
        } else {
            LOGGER.error("SetSpriteIndexAction: No sprite animation state found for element: " + action.getElementId());
        }
    }

    private static void handleStepSpriteIndex(StepSpriteIndexAction action) {
        SpriteState spriteState = spriteStates.get(action.getElementId());
        GuiElement activeElement = activeElements.get(action.getElementId());
        if (spriteState != null && activeElement != null) {
            int currentSpriteIndex = spriteState.getCurrentIndex();
            int targetIndex = currentSpriteIndex + action.getSteps();

            if (!action.shouldLoop() && (targetIndex < 0 || targetIndex >= spriteState.getSprites().size())) {
                LOGGER.warn("StepSpriteAction: Stepping out of bounds for non-looping sprite.");
                return;
            }

            animatedStepStates.put(action.getElementId(), new AnimatedStepState(
                    System.currentTimeMillis() - animationStartTime,
                    currentSpriteIndex,
                    targetIndex,
                    action.getDuration(),
                    action.shouldLoop(),
                    spriteState.getSprites().size(),
                    action.getSteps() // Pass the steps value
            ));
        } else {
            LOGGER.error("StepSpriteAction: No sprite animation state or active element found for: " + action.getElementId());
        }
    }


    private static void handleStopSpriteAnimation(StopSpriteAnimationAction action) {
        spriteUpdateIntervals.remove(action.getElementId());
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

    private static void handleShowElement(ShowElementAction action) {
        GuiElementData elementData = currentAnimation.getElementById(action.getElementId());
        if (elementData != null) {
            ResourceLocation textureResource = null;
            if (elementData.getTexture() != null) {
                textureResource = AssetManager.loadTexture(currentAnimation.getNamespace(), elementData.getTexture());
            } else if (elementData.getTexturePathPattern() != null) {
                List<ResourceLocation> sprites = SpriteManager.loadSprites(currentAnimation.getNamespace(), elementData.getTexturePathPattern());
                if (!sprites.isEmpty()) {
                    spriteStates.put(elementData.getId(), new SpriteState(sprites));
                    textureResource = sprites.get(0);
                } else {
                    LOGGER.error("Failed to load sprites for element: {}", elementData.getId());
                    return;
                }
            } else {
                LOGGER.error("Element has no texture or texture pattern: {}", elementData.getId());
                return;
            }

            if (textureResource != null) {
                GuiElement newElement = new GuiElement(textureResource, action.getX(), action.getY(), action.getWidth(), action.getHeight(), action.getTextureWidth(), action.getTextureHeight());
                if (action.getStartOpacity() != null) {
                    newElement.setOpacity(action.getStartOpacity());
                    if (action.getFadeDuration() != null && action.getFadeDuration() > 0) {
                        fadingStates.put(action.getElementId(), new FadeState(
                                System.currentTimeMillis() - animationStartTime,
                                action.getStartOpacity(),
                                action.getTargetOpacity() != null ? action.getTargetOpacity() : 1.0f,
                                action.getFadeDuration(),
                                action.getFadeEasingType() != null ? action.getFadeEasingType() : "linear"
                        ));
                    }
                }

                activeElements.put(action.getElementId(), newElement);
            } else {
                LOGGER.error("Failed to load texture for element: {}", elementData.getId());
            }
        } else {
            LOGGER.warn("ShowElementAction for non-existent element: {}", action.getElementId());
        }
    }

    private static void handlePlaySound(PlaySoundAction action) {
        File soundFile = AssetManager.loadSound(currentAnimation.getNamespace(), action.getSoundId());
        if (soundFile != null) {
            RawAudio.playOgg(soundFile.getAbsolutePath(), action.getVolume());
        } else {
            LOGGER.error("Failed to load sound file: {}:{}", currentAnimation.getNamespace(), action.getSoundId());
        }
    }


    private static void handleHideElement(HideElementAction action) {
        try {
            activeElements.remove(action.getElementId());
            animatedStepStates.remove(action.getElementId());
            spriteUpdateIntervals.remove(action.getElementId());
            spriteStates.remove(action.getElementId());
        } catch (NullPointerException e) {
        } catch (Exception e) {
            LOGGER.error("Failed to hide element: {}", action.getElementId());
            e.printStackTrace();
        }
    }

    public static Map<String, GuiElement> getActiveElements() {
        Map<String, GuiElement> allElements = new HashMap<>();
        allElements.putAll(activeElements);
        allElements.putAll(temporaryElements);
        return new TreeMap<>(allElements);
    }
}