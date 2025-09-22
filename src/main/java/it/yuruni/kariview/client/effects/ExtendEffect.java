package it.yuruni.kariview.client.effects;

import com.google.gson.annotations.SerializedName;

public class ExtendEffect extends AudioEffect {
    private final String direction;
    @SerializedName("target_value")
    private final double targetValue;
    private final double decay;
    @SerializedName("default_value")
    private final double defaultValue;
    @SerializedName("extend_duration")
    private final double extendDuration;

    public ExtendEffect(String direction, double targetValue, double decay, double defaultValue, double extendDuration) {
        super("EXTEND");
        this.direction = direction;
        this.targetValue = targetValue;
        this.decay = decay;
        this.defaultValue = defaultValue;
        this.extendDuration = extendDuration;
    }

    public String getDirection() {
        return direction;
    }

    public double getTargetValue() {
        return targetValue;
    }

    public double getDecay() {
        return decay;
    }

    public double getDefaultValue() {
        return defaultValue;
    }

    public double getExtendTime() {
        return extendDuration;
    }
}
