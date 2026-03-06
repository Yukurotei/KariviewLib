package it.yuruni.kariview.client.data.actions;

import com.google.gson.annotations.SerializedName;
import it.yuruni.kariview.client.animation.states.AnimationContext;

public class StopSpriteAnimationAction implements Action {
    @SerializedName("element_id")
    private String elementId;

    public String getElementId() {
        return elementId;
    }

    @Override
    public void execute(AnimationContext ctx) {
        ctx.spriteUpdateIntervals.remove(elementId);
    }
}
