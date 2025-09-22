package it.yuruni.kariview.client.effects;

import com.google.gson.annotations.SerializedName;

public class PulseEffect extends AudioEffect {

    @SerializedName("decay")
    private double decay;
    @SerializedName("default_scale")
    private float defaultValue;
    @SerializedName("max_scale")
    private float maxValue;

    public PulseEffect(String type) {
        super(type);
    }

    public double getDecay() {
        return decay;
    }

    public float getDefaultValue() {
        return defaultValue;
    }

    public float getMaxValue() {
        return maxValue;
    }
}
