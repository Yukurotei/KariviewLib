package it.yuruni.kariview.client.data.actions;

import com.google.gson.annotations.SerializedName;
import it.yuruni.kariview.client.effects.AudioEffect;

import java.util.List;

public class RegisterAudioElementAction implements Action {
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
    private String direction;
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

    public String getDirection() {
        return direction.toUpperCase();
    }

    public String getEasingType() {
        return easingType;
    }
}