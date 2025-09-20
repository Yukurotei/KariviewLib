package it.yuruni.kariview.client.effects;

import com.google.gson.annotations.SerializedName;

public class PulseEffect extends AudioEffect {

    @SerializedName("decay_duration")
    private float decay;
    @SerializedName("default_scale")
    private float defaultValue;
    @SerializedName("max_scale")
    private float maxValue;

    public PulseEffect(String type) {
        super(type);
    }

    public float getDecay() {
        return decay;
    }

    public float getDefaultValue() {
        return defaultValue;
    }

    public float getMaxValue() {
        return maxValue;
    }
}
