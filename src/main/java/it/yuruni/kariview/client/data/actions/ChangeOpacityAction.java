package it.yuruni.kariview.client.data.actions;

import com.google.gson.annotations.SerializedName;
import it.yuruni.kariview.client.GuiElement;
import it.yuruni.kariview.client.animation.states.AnimationContext;
import it.yuruni.kariview.client.animation.states.FadeState;

public class ChangeOpacityAction implements Action {
    @SerializedName("element_id")
    private String elementId;
    @SerializedName("target_opacity")
    private float targetOpacity;
    private long duration;
    @SerializedName("easing_type")
    private String easingType;

    public String getElementId() { return elementId; }
    public float getTargetOpacity() { return targetOpacity; }
    public long getDuration() { return duration; }
    public String getEasingType() { return easingType; }

    public void setElementId(String elementId) { this.elementId = elementId; }
    public void setTargetOpacity(float targetOpacity) { this.targetOpacity = targetOpacity; }
    public void setDuration(long duration) { this.duration = duration; }
    public void setEasingType(String easingType) { this.easingType = easingType; }

    @Override
    public void execute(AnimationContext ctx) {
        GuiElement element = ctx.activeElements.get(elementId);
        if (element == null) return;
        ctx.fadingStates.put(elementId, new FadeState(
                System.currentTimeMillis() - ctx.animationStartTime,
                element.getOpacity(),
                targetOpacity,
                duration,
                easingType != null ? easingType : "linear"
        ));
    }
}
