package it.yuruni.kariview.client.animation;

import com.mojang.logging.LogUtils;
import it.yuruni.kariview.client.GuiElement;
import it.yuruni.kariview.client.KariviewRenderer;
import it.yuruni.kariview.client.data.AnimationData;
import it.yuruni.kariview.client.data.AnimationLoader;
import it.yuruni.kariview.client.data.Keyframe;
import it.yuruni.kariview.client.data.actions.*;
import it.yuruni.kariview.client.data.elements.GuiElementData;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class AnimationManager {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static AnimationData currentAnimation;
    private static long animationStartTime;
    private static final ConcurrentMap<String, GuiElement> activeElements = new ConcurrentHashMap<>();
    private static int lastKeyframeIndex = -1;

    private static final Map<String, SpriteState> spriteStates = new HashMap<>();
    private static final Map<String, Long> spriteUpdateIntervals = new HashMap<>();
    private static final Map<String, AnimatedStepState> animatedStepStates = new HashMap<>();
    private static final Map<String, ScaleState> scalingStates = new HashMap<>();

    private static class ScaleState {
        private final long startTime;
        private final double startScale;
        private final double targetScale;
        private final long duration;

        public ScaleState(long startTime, double startScale, double targetScale, long duration) {
            this.startTime = startTime;
            this.startScale = startScale;
            this.targetScale = targetScale;
            this.duration = duration;
        }

        public long getStartTime() { return startTime; }
        public double getStartScale() { return startScale; }
        public double getTargetScale() { return targetScale; }
        public long getDuration() { return duration; }
    }

    public static void startAnimation(AnimationData animationData) {
        currentAnimation = animationData;
        animationStartTime = System.currentTimeMillis();
        KariviewRenderer.isGuiActive = true;
        lastKeyframeIndex = -1;
        activeElements.clear();
        spriteStates.clear();
        spriteUpdateIntervals.clear();
    }

    public static void playAnimation(String namespace, String animationId) {
        AnimationData data = AnimationLoader.loadAnimation(namespace, animationId);
        if (data != null) {
            LOGGER.info("Starting animation: {}", data.getId());
            currentAnimation = data;
            animationStartTime = System.currentTimeMillis();
            KariviewRenderer.isGuiActive = true;
            activeElements.clear(); // Clear old elements
        } else {
            LOGGER.error("Failed to load animation: {}:{}", namespace, animationId);
        }
    }

    public static void tick() {
        if (currentAnimation == null) {
            return;
        }

        long elapsed = System.currentTimeMillis() - animationStartTime;

        //Check if step animating is finished
        animatedStepStates.entrySet().removeIf(entry -> {
            String elementId = entry.getKey();
            AnimatedStepState state = entry.getValue();
            GuiElement activeElement = activeElements.get(elementId);

            if (activeElement == null) {
                return true; // Remove if element is no longer active
            }

            long elapsedSinceStart = elapsed - state.getStartTime();
            if (elapsedSinceStart >= state.getDuration()) {
                // Animation is complete, set to final frame and remove state
                int finalIndex = state.shouldLoop() ? (state.getTargetSpriteIndex() + state.getTotalSprites()) % state.getTotalSprites() : state.getTargetSpriteIndex();
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

        //Check if scaling animation is finished
        scalingStates.entrySet().removeIf(entry -> {
            String elementId = entry.getKey();
            ScaleState state = entry.getValue();
            GuiElement element = activeElements.get(elementId);
            if (element == null) {
                return true;
            }

            long elapsedSinceStart = (System.currentTimeMillis() - animationStartTime) - state.getStartTime();
            if (elapsedSinceStart >= state.getDuration()) {
                element.setScale(state.getTargetScale());
                return true;
            }

            double progress = (double) elapsedSinceStart / state.getDuration();
            double newScale = state.getStartScale() + (state.getTargetScale() - state.getStartScale()) * progress;
            element.setScale(newScale);

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

                // Update the texture of the active element directly
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
        int totalSteps = state.getTotalSteps();
        int currentStep = (int) (elapsedSinceStart / state.getDuration() * totalSteps);

        int direction = state.getTargetSpriteIndex() >= state.getStartSpriteIndex() ? 1 : -1;
        if (state.shouldLoop() && state.getTargetSpriteIndex() < state.getStartSpriteIndex()) {
            direction = 1;
        }

        int newIndex;
        if (state.shouldLoop()) {
            newIndex = (state.getStartSpriteIndex() + currentStep) % state.getTotalSprites();
        } else {
            newIndex = state.getStartSpriteIndex() + (currentStep * direction);
            if ((direction > 0 && newIndex > state.getTargetSpriteIndex()) || (direction < 0 && newIndex < state.getTargetSpriteIndex())) {
                newIndex = state.getTargetSpriteIndex();
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

    private static void executeKeyframeActions(List<Action> actions) {
        if (actions == null) {
            return;
        }

        for (Action action : actions) {
            LOGGER.info(action.toString());
            if (action instanceof ShowElementAction) {
                handleShowElement((ShowElementAction) action);
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
            }
        }
    }

    private static void handleScaleAction(ScaleAction action) {
        GuiElement element = activeElements.get(action.getElementId());
        if (element != null) {
            scalingStates.put(action.getElementId(), new ScaleState(
                    System.currentTimeMillis() - animationStartTime,
                    element.getScale(),
                    action.getTargetScale(),
                    action.getDuration()
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

    private static class AnimatedStepState {
        private final long startTime;
        private final int startSpriteIndex;
        private final int targetSpriteIndex;
        private final long duration;
        private final boolean loop;
        private final int totalSprites;
        private final int totalSteps;

        public AnimatedStepState(long startTime, int startSpriteIndex, int targetSpriteIndex, long duration, boolean loop, int totalSprites, int totalSteps) {
            this.startTime = startTime;
            this.startSpriteIndex = startSpriteIndex;
            this.targetSpriteIndex = targetSpriteIndex;
            this.duration = duration;
            this.loop = loop;
            this.totalSprites = totalSprites;
            this.totalSteps = totalSteps;
        }

        // Getters
        public long getStartTime() { return startTime; }
        public int getStartSpriteIndex() { return startSpriteIndex; }
        public int getTargetSpriteIndex() { return targetSpriteIndex; }
        public long getDuration() { return duration; }
        public boolean shouldLoop() { return loop; }
        public int getTotalSprites() { return totalSprites; }
        public int getTotalSteps() { return totalSteps; }
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
                // If a texture pattern exists, load the sprites and set the initial texture.
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
            return;
        } catch (Exception e) {
            LOGGER.error("Failed to hide element: {}", action.getElementId());
            e.printStackTrace();
        }
    }

    public static ConcurrentMap<String, GuiElement> getActiveElements() {
        return activeElements;
    }
}