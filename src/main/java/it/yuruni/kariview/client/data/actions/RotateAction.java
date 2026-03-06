package it.yuruni.kariview.client.data.actions;

import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import it.yuruni.kariview.client.GuiElement;
import it.yuruni.kariview.client.animation.states.AnimationContext;
import it.yuruni.kariview.client.animation.states.RotateState;
import org.slf4j.Logger;

public class RotateAction implements Action {
    private static final Logger LOGGER = LogUtils.getLogger();

    @SerializedName("element_id")
    private String elementId;
    @SerializedName("target_angle")
    private double targetAngle;
    @SerializedName("duration")
    private long duration;
    @SerializedName("easing_type")
    private String easingType;

    public String getElementId() {
        return elementId;
    }

    public double getTargetAngle() {
        return targetAngle;
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
            ctx.rotatingStates.put(elementId, new RotateState(
                    System.currentTimeMillis() - ctx.animationStartTime,
                    element.getAngle(),
                    targetAngle,
                    duration,
                    easingType
            ));
        } else {
            LOGGER.error("RotateAction: No active element found for id: {}", elementId);
        }
    }
}
