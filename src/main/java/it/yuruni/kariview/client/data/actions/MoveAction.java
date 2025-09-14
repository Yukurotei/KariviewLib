package it.yuruni.kariview.client.data.actions;

import com.google.gson.annotations.SerializedName;

public class MoveAction implements Action {
    @SerializedName("element_id")
    private String elementId;
    @SerializedName("target_x")
    private double targetX;
    @SerializedName("target_y")
    private double targetY;
    @SerializedName("duration")
    private long duration;
    @SerializedName("easing_type")
    private String easingType;

    public String getElementId() {
        return elementId;
    }

    public double getTargetX() {
        return targetX;
    }

    public double getTargetY() {
        return targetY;
    }

    public long getDuration() {
        return duration;
    }

    public String getEasingType() {
        return easingType;
    }
}