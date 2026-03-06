package it.yuruni.kariview.client.data.actions;

import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import it.yuruni.kariview.client.GuiElement;
import it.yuruni.kariview.client.animation.states.AnimatedStepState;
import it.yuruni.kariview.client.animation.states.AnimationContext;
import it.yuruni.kariview.client.animation.SpriteState;
import org.slf4j.Logger;

public class StepSpriteIndexAction implements Action {
    private static final Logger LOGGER = LogUtils.getLogger();

    @SerializedName("element_id")
    private String elementId;

    @SerializedName("steps")
    private int steps;

    @SerializedName("loop")
    private boolean shouldLoop = true;

    @SerializedName("duration")
    private long duration;

    public String getElementId() {
        return elementId;
    }

    public int getSteps() {
        return steps;
    }

    public boolean shouldLoop() {
        return shouldLoop;
    }

    public long getDuration() {
        return duration;
    }

    @Override
    public void execute(AnimationContext ctx) {
        SpriteState spriteState = ctx.spriteStates.get(elementId);
        GuiElement activeElement = ctx.activeElements.get(elementId);
        if (spriteState != null && activeElement != null) {
            int currentSpriteIndex = spriteState.getCurrentIndex();
            int targetIndex = currentSpriteIndex + steps;

            if (!shouldLoop && (targetIndex < 0 || targetIndex >= spriteState.getSprites().size())) {
                LOGGER.warn("StepSpriteAction: Stepping out of bounds for non-looping sprite.");
                return;
            }

            ctx.animatedStepStates.put(elementId, new AnimatedStepState(
                    System.currentTimeMillis() - ctx.animationStartTime,
                    currentSpriteIndex,
                    targetIndex,
                    duration,
                    shouldLoop,
                    spriteState.getSprites().size(),
                    steps
            ));
        } else {
            LOGGER.error("StepSpriteAction: No sprite animation state or active element found for: " + elementId);
        }
    }
}
