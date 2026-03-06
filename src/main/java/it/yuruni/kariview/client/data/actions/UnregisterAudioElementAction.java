package it.yuruni.kariview.client.data.actions;

import com.google.gson.annotations.SerializedName;
import it.yuruni.kariview.client.animation.states.AnimationContext;
import it.yuruni.kariview.client.sound.BeatDetector;

public class UnregisterAudioElementAction implements Action {
    @SerializedName("element_id")
    private String elementId;

    public String getElementId() {
        return elementId;
    }

    @Override
    public void execute(AnimationContext ctx) {
        BeatDetector.registeredAudioElements.remove(elementId);
    }
}
