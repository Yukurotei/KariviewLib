package it.yuruni.kariview.client.data.actions;

import com.google.gson.annotations.SerializedName;

public class ScaleAction implements Action {
    @SerializedName("element_id")
    private String elementId;
    @SerializedName("target_scale")
    private double targetScale;
    @SerializedName("duration")
    private long duration;

    public String getElementId() {
        return elementId;
    }

    public double getTargetScale() {
        return targetScale;
    }

    public long getDuration() {
        return duration;
    }
}
