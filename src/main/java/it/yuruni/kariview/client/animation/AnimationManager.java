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
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class AnimationManager {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static AnimationData currentAnimation;
    private static long animationStartTime;
    private static final ConcurrentMap<String, GuiElement> activeElements = new ConcurrentHashMap<>();
    private static int lastKeyframeIndex = -1;

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

        if (elapsed > currentAnimation.getTotalDuration()) {
            KariviewRenderer.isGuiActive = false;
            currentAnimation = null;
            activeElements.clear();
            lastKeyframeIndex = -1;
            return;
        }

        List<Keyframe> keyframes = currentAnimation.getKeyframes();
        for (int i = lastKeyframeIndex + 1; i < keyframes.size(); i++) {
            Keyframe keyframe = keyframes.get(i);
            if (elapsed >= keyframe.getTimestamp()) {
                executeKeyframeActions(keyframe.getActions());
                lastKeyframeIndex = i;
            } else {
                // Keyframes are sorted by timestamp, so we can stop early
                break;
            }
        }
    }



    private static void executeKeyframeActions(List<Action> actions) {
        if (actions == null) {
            return;
        }

        for (Action action : actions) {
            if (action instanceof ShowElementAction) {
                handleShowElement((ShowElementAction) action);
            } else if (action instanceof HideElementAction) {
                handleHideElement((HideElementAction) action);
            }  else if (action instanceof PlaySoundAction) {
                handlePlaySound((PlaySoundAction) action);
            }
        }
    }

    private static void handleShowElement(ShowElementAction action) {
        GuiElementData elementData = currentAnimation.getElementById(action.getElementId());
        if (elementData != null) {
            ResourceLocation textureResource = AssetManager.loadTexture(currentAnimation.getNamespace(), elementData.getTexture());
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
            RawAudio.playOgg(soundFile.getAbsolutePath());
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