package it.yuruni.kariview.client.data.actions;

import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import it.yuruni.kariview.client.GuiElement;
import it.yuruni.kariview.client.animation.states.AnimationContext;
import it.yuruni.kariview.client.animation.SpriteState;
import org.slf4j.Logger;

public class SetSpriteIndexAction implements Action {
    private static final Logger LOGGER = LogUtils.getLogger();

    @SerializedName("element_id")
    private String elementId;

    @SerializedName("target_index")
    private int targetIndex;

    @SerializedName("loop")
    private boolean shouldLoop = true;

    public String getElementId() {
        return elementId;
    }

    public int getTargetIndex() {
        return targetIndex;
    }

    public boolean shouldLoop() {
        return shouldLoop;
    }

    @Override
    public void execute(AnimationContext ctx) {
        SpriteState spriteState = ctx.spriteStates.get(elementId);
        GuiElement activeElement = ctx.activeElements.get(elementId);
        if (spriteState != null && activeElement != null) {
            int newIndex = targetIndex;
            if (!shouldLoop && (newIndex < 0 || newIndex >= spriteState.getSprites().size())) {
                LOGGER.warn("SetSpriteIndexAction: Target index " + newIndex + " is out of bounds for non-looping sprite.");
                return;
            }
            spriteState.setCurrentIndex(newIndex % spriteState.getSprites().size());
            activeElement.setTexture(spriteState.getCurrentSprite());
        } else {
            LOGGER.error("SetSpriteIndexAction: No sprite animation state found for element: " + elementId);
        }
    }
}
