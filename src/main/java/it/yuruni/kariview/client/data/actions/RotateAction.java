package it.yuruni.kariview.client.data.actions;

import com.google.gson.annotations.SerializedName;

public class RotateAction implements Action {
    @SerializedName("element_id")
    private String elementId;
    @SerializedName("target_angle")
    private double targetAngle;
    @SerializedName("duration")
    private long duration;
    @SerializedName("easing_type")
    private String easingType;

    public String getElementId() {
        return elementId;
    }

    public double getTargetAngle() {
        return targetAngle;
    }

    public long getDuration() {
        return duration;
    }

    public String getEasingType() {
        return easingType;
    }
}
