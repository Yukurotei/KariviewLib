package it.yuruni.kariview.client.data.actions;

import com.google.gson.annotations.SerializedName;
import it.yuruni.kariview.client.animation.AnimationManager;
import it.yuruni.kariview.client.animation.states.AnimationContext;

public class PlayAnimationAction implements Action {
    private String namespace;
    @SerializedName("animation_id")
    private String animationId;
    @SerializedName("delay_ms")
    private long delayMs = 0;

    @Override
    public void execute(AnimationContext ctx) {
        AnimationManager.scheduleAnimation(namespace, animationId, delayMs);
    }
}
