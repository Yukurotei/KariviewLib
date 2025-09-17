package it.yuruni.kariview.client.data.actions;

import com.google.gson.annotations.SerializedName;

public class ExtendAction implements Action {
    @SerializedName("element_id")
    private String elementId;
    @SerializedName("target_value")
    private double targetValue;
    private long duration;
    private String direction;
    @SerializedName("easing_type")
    private String easingType;

    public String getElementId() {
        return elementId;
    }

    public double getTargetValue() {
        return targetValue;
    }

    public long getDuration() {
        return duration;
    }

    public String getDirection() {
        return direction.toUpperCase();
    }

    public String getEasingType() {
        return easingType;
    }
}
