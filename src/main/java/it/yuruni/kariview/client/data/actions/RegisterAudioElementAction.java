package it.yuruni.kariview.client.data.actions;

import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import it.yuruni.kariview.client.animation.states.AnimationContext;
import it.yuruni.kariview.client.effects.AudioEffect;
import it.yuruni.kariview.client.sound.BeatDetector;
import org.slf4j.Logger;

import java.util.List;

public class RegisterAudioElementAction implements Action {
    private static final Logger LOGGER = LogUtils.getLogger();

    @SerializedName("element_id")
    private String elementId;
    private float sensitivity;
    @SerializedName("max_hertz")
    private int maxHertz;
    @SerializedName("min_hertz")
    private int minHertz;
    @SerializedName("max_volume")
    private float maxVolume;
    private List<AudioEffect> effects;
    @SerializedName("easing_type")
    private String easingType;

    public String getElementId() {
        return elementId;
    }

    public float getSensitivity() {
        return sensitivity;
    }

    public int getMaxHertz() {
        return maxHertz;
    }

    public int getMinHertz() {
        return minHertz;
    }

    public float getMaxVolume() {
        return maxVolume;
    }

    public List<AudioEffect> getEffects() {
        return effects;
    }

    public String getEasingType() {
        return easingType;
    }

    @Override
    public void execute(AnimationContext ctx) {
        if (ctx.activeElements.containsKey(elementId)) {
            BeatDetector.registeredAudioElements.put(elementId, this);
        } else {
            LOGGER.error("Cannot register audio element. Element not found: {}", elementId);
        }
    }
}
