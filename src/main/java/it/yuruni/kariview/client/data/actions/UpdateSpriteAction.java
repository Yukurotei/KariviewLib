package it.yuruni.kariview.client.data.actions;

import com.google.gson.annotations.SerializedName;

public class UpdateSpriteAction implements Action {
    @SerializedName("element_id")
    private String elementId;

    @SerializedName("update_interval")
    private long updateInterval;

    @SerializedName("loop")
    private boolean shouldLoop = true;

    public String getElementId() {
        return elementId;
    }

    public long getUpdateInterval() {
        return updateInterval;
    }

    public boolean shouldLoop() {
        return shouldLoop;
    }
}