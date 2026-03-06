package it.yuruni.kariview.client.data.actions;

import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import it.yuruni.kariview.client.GuiElement;
import it.yuruni.kariview.client.animation.states.AnimationContext;
import it.yuruni.kariview.client.animation.states.ScaleState;
import org.slf4j.Logger;

public class ScaleAction implements Action {
    private static final Logger LOGGER = LogUtils.getLogger();

    @SerializedName("element_id")
    private String elementId;
    @SerializedName("target_scale")
    private double targetScale;
    @SerializedName("duration")
    private long duration;
    @SerializedName("easing_type")
    private String easingType;

    public String getElementId() {
        return elementId;
    }

    public double getTargetScale() {
        return targetScale;
    }

    public long getDuration() {
        return duration;
    }

    public String getEasingType() {
        return easingType;
    }

    @Override
    public void execute(AnimationContext ctx) {
        GuiElement element = ctx.activeElements.get(elementId);
        if (element != null) {
            ctx.scalingStates.put(elementId, new ScaleState(
                    System.currentTimeMillis() - ctx.animationStartTime,
                    element.getXScale(),
                    element.getYScale(),
                    element.getXScale() < targetScale ? targetScale / 2 : targetScale,
                    element.getYScale() < targetScale ? targetScale / 2 : targetScale,
                    duration,
                    "ALL",
                    element.getX(),
                    element.getY(),
                    easingType
            ));
        } else {
            LOGGER.error("ScaleAction: No active element found for id: {}", elementId);
        }
    }
}
