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

        if (lastKeyframeIndex >= keyframes.size() - 1) {
            stopAllAnimations();
        }
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
            }
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
        if (spriteState != null) {
            int newIndex = action.getTargetIndex();
            if (!action.shouldLoop() && (newIndex < 0 || newIndex >= spriteState.getSprites().size())) {
                LOGGER.warn("SetSpriteIndexAction: Target index " + newIndex + " is out of bounds for non-looping sprite.");
                return;
            }
            spriteState.setCurrentIndex(newIndex % spriteState.getSprites().size());
        } else {
            LOGGER.error("SetSpriteIndexAction: No sprite animation state found for element: " + action.getElementId());
        }
    }

    private static void handleStepSpriteIndex(StepSpriteIndexAction action) {
        SpriteState spriteState = spriteStates.get(action.getElementId());
        if (spriteState != null) {
            int newIndex = spriteState.getCurrentIndex() + action.getSteps();
            if (action.shouldLoop()) {
                newIndex = (newIndex + spriteState.getSprites().size()) % spriteState.getSprites().size();
            } else if (newIndex < 0 || newIndex >= spriteState.getSprites().size()) {
                LOGGER.warn("StepSpriteIndexAction: Stepping out of bounds for non-looping sprite.");
                return;
            }
            spriteState.setCurrentIndex(newIndex);
        } else {
            LOGGER.error("StepSpriteIndexAction: No sprite animation state found for element: " + action.getElementId());
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