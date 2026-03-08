package it.yuruni.kariview.client.data.actions;

import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import it.yuruni.kariview.client.GuiElement;
import it.yuruni.kariview.client.animation.states.AnimationContext;
import it.yuruni.kariview.client.animation.states.ScaleState;
import org.slf4j.Logger;

public class ExtendAction implements Action {
    private static final Logger LOGGER = LogUtils.getLogger();

    @SerializedName("element_id")
    private String elementId;
    @SerializedName("target_value")
    private double targetValue;
    private long duration;
    private String direction;
    @SerializedName("easing_type")
    private String easingType;

    public String getElementId() {
        return elementId;
    }

    public double getTargetValue() {
        return targetValue;
    }

    public long getDuration() {
        return duration;
    }

    public String getDirection() {
        return direction.toUpperCase();
    }

    public String getEasingType() {
        return easingType;
    }

    public void setElementId(String elementId) { this.elementId = elementId; }
    public void setTargetValue(double targetValue) { this.targetValue = targetValue; }
    public void setDuration(long duration) { this.duration = duration; }
    public void setDirection(String direction) { this.direction = direction; }
    public void setEasingType(String easingType) { this.easingType = easingType; }

    @Override
    public void execute(AnimationContext ctx) {
        GuiElement element = ctx.activeElements.get(elementId);
        if (element != null) {
            double targetX = element.getXScale();
            double targetY = element.getYScale();
            switch (direction.toUpperCase()) {
                case "LEFT":
                case "RIGHT":
                    targetX = targetValue;
                    break;
                case "UP":
                case "DOWN":
                    targetY = targetValue;
                    break;
            }
            ctx.scalingStates.put(elementId, new ScaleState(
                    System.currentTimeMillis() - ctx.animationStartTime,
                    element.getXScale(),
                    element.getYScale(),
                    targetX,
                    targetY,
                    duration,
                    direction.toUpperCase(),
                    element.getX(),
                    element.getY(),
                    easingType
            ));
        } else {
            LOGGER.error("ExtendAction: No active element found for id: {}", elementId);
        }
    }
}
