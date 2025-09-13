package it.yuruni.kariview.client.animation;

import com.mojang.logging.LogUtils;
import it.yuruni.kariview.Kariview;
import it.yuruni.kariview.client.GuiElement;
import it.yuruni.kariview.client.KariviewRenderer;
import it.yuruni.kariview.client.data.AnimationData;
import it.yuruni.kariview.client.data.AnimationLoader;
import it.yuruni.kariview.client.data.Keyframe;
import it.yuruni.kariview.client.data.actions.*;
import it.yuruni.kariview.client.data.elements.GuiElementData;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class AnimationManager {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static AnimationData currentAnimation;
    private static long animationStartTime;
    private static final ConcurrentMap<String, GuiElement> activeElements = new ConcurrentHashMap<>();

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
        if (currentAnimation == null) return;

        long elapsed = System.currentTimeMillis() - animationStartTime;
        LOGGER.info("Ticking animation: {}", currentAnimation.getId());

        for (Keyframe keyframe : currentAnimation.getKeyframes()) {
            if (elapsed >= keyframe.getTimestamp()) {
                executeKeyframeActions(keyframe.getActions());
            }
        }
    }

    private static void executeKeyframeActions(List<Action> actions) {
        for (Action action : actions) {
            LOGGER.info("{}", action);
            if (action instanceof ShowElementAction) {
                handleShowElement((ShowElementAction) action);
            } else if (action instanceof HideElementAction) {
                handleHideElement((HideElementAction) action);
            }
        }
    }

    private static void handleShowElement(ShowElementAction action) {
        GuiElementData elementData = currentAnimation.getElementById(action.getElementId());
        if (elementData != null) {
            ResourceLocation textureResource = new ResourceLocation(Kariview.MODID, elementData.getTexture());
            LOGGER.info(textureResource.getNamespace() + ":" + textureResource.getPath());
            GuiElement newElement = new GuiElement(textureResource, action.getX(), action.getY(), action.getWidth(), action.getHeight(), action.getTextureWidth(), action.getTextureHeight());
            activeElements.put(action.getElementId(), newElement);
        } else {
            LOGGER.warn("ShowElementAction for non-existent element: {}", action.getElementId());
        }
    }

    private static void handleHideElement(HideElementAction action) {
        activeElements.remove(action.getElementId());
    }

    public static ConcurrentMap<String, GuiElement> getActiveElements() {
        return activeElements;
    }
}