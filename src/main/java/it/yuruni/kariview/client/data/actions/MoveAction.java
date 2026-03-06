package it.yuruni.kariview.client.data.actions;

import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import it.yuruni.kariview.client.GuiElement;
import it.yuruni.kariview.client.animation.states.AnimationContext;
import it.yuruni.kariview.client.animation.states.MoveState;
import org.slf4j.Logger;

public class MoveAction implements Action {
    private static final Logger LOGGER = LogUtils.getLogger();

    @SerializedName("element_id")
    private String elementId;
    @SerializedName("target_x")
    private double targetX;
    @SerializedName("target_y")
    private double targetY;
    @SerializedName("duration")
    private long duration;
    @SerializedName("easing_type")
    private String easingType;

    public String getElementId() {
        return elementId;
    }

    public double getTargetX() {
        return targetX;
    }

    public double getTargetY() {
        return targetY;
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
            ctx.movingStates.put(elementId, new MoveState(
                    System.currentTimeMillis() - ctx.animationStartTime,
                    element.getX(),
                    element.getY(),
                    targetX,
                    targetY,
                    duration,
                    easingType
            ));
        } else {
            LOGGER.error("MoveAction: No active element found for id: {}", elementId);
        }
    }
}
