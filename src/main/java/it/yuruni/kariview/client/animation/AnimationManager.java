package it.yuruni.kariview.client.animation;

import it.yuruni.kariview.client.data.AnimationData;
import it.yuruni.kariview.client.data.AnimationLoader;
import it.yuruni.kariview.client.data.Keyframe;
import it.yuruni.kariview.client.data.elements.GuiElementData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class AnimationManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static AnimationData activeAnimation = null;
    private static long animationStartTime = 0;
    private static List<GuiElementData> activeElements = new ArrayList<>();
    private static int currentKeyframeIndex = -1;

    public static void playAnimation(String namespace, String animationId) {
        try {
            activeAnimation = AnimationLoader.loadAnimation(namespace, animationId);
            if (activeAnimation != null) {
                animationStartTime = System.currentTimeMillis();
                activeElements.clear();
                currentKeyframeIndex = 0;
                LOGGER.info("Successfully started playing animation: " + activeAnimation.getId());
            } else {
                LOGGER.error("Failed to load animation: " + namespace + ":" + animationId);
            }
        } catch (Exception e) {
            LOGGER.error("Error playing animation " + namespace + ":" + animationId, e);
        }
    }

    public static void tick() {
        if (activeAnimation == null) {
            return;
        }

        long elapsedTime = System.currentTimeMillis() - animationStartTime;

        if (currentKeyframeIndex < activeAnimation.getKeyframes().size()) {
            Keyframe currentKeyframe = activeAnimation.getKeyframes().get(currentKeyframeIndex);
            if (elapsedTime >= currentKeyframe.getTimestamp()) {
                executeKeyframeActions(currentKeyframe);
                currentKeyframeIndex++;
            }
        }
    }

    private static void executeKeyframeActions(Keyframe keyframe) {
        // This is a placeholder for the logic to execute actions
        // like showing elements or playing sounds.
        // We will implement this in the next steps.
    }

    public static List<GuiElementData> getActiveElements() {
        return activeElements;
    }
}
