package it.yuruni.kariview.client.data.actions;

import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import it.yuruni.kariview.client.animation.states.AnimationContext;
import org.slf4j.Logger;

public class HideElementAction implements Action {
    private static final Logger LOGGER = LogUtils.getLogger();

    @SerializedName("element_id")
    private String elementId;

    public String getElementId() {
        return elementId;
    }

    public void setElementId(String elementId) { this.elementId = elementId; }

    @Override
    public void execute(AnimationContext ctx) {
        try {
            ctx.activeElements.remove(elementId);
            ctx.animatedStepStates.remove(elementId);
            ctx.spriteUpdateIntervals.remove(elementId);
            ctx.spriteStates.remove(elementId);
        } catch (NullPointerException e) {
        } catch (Exception e) {
            LOGGER.error("Failed to hide element: {}", elementId);
            e.printStackTrace();
        }
    }
}
