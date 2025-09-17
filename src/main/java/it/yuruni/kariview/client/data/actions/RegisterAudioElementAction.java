package it.yuruni.kariview.client.data.actions;

import com.google.gson.annotations.SerializedName;

public class RegisterAudioElementAction implements Action {
    @SerializedName("element_id")
    private String elementId;
    private float sensitivity;
    @SerializedName("max_hertz")
    private int maxHertz;
    @SerializedName("min_hertz")
    private int minHertz;
    @SerializedName("max_value")
    private float maxValue;
    @SerializedName("max_volume")
    private float maxVolume;
    private String effect;
    private String direction;
    private float decay;
    @SerializedName("default_value")
    private float defaultValue;

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

    public float getMaxValue() {
        return maxValue;
    }

    public float getMaxVolume() {
        return maxVolume;
    }

    public String getEffect() {
        return effect;
    }

    public String getDirection() {
        return direction;
    }

    public float getDecay() {
        return decay;
    }

    public float getDefaultValue() {
        return defaultValue;
    }
}