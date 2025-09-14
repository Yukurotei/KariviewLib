package it.yuruni.kariview.client.data.actions;

import com.google.gson.annotations.SerializedName;

public class StepSpriteIndexAction implements Action {
    @SerializedName("element_id")
    private String elementId;

    @SerializedName("steps")
    private int steps;

    @SerializedName("loop")
    private boolean shouldLoop = true;

    @SerializedName("duration")
    private long duration;

    public String getElementId() {
        return elementId;
    }

    public int getSteps() {
        return steps;
    }

    public boolean shouldLoop() {
        return shouldLoop;
    }

    public long getDuration() {
        return duration;
    }
}
