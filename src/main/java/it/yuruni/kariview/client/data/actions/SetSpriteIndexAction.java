package it.yuruni.kariview.client.data.actions;

import com.google.gson.annotations.SerializedName;

public class SetSpriteIndexAction implements Action {
    @SerializedName("element_id")
    private String elementId;

    @SerializedName("target_index")
    private int targetIndex;

    @SerializedName("loop")
    private boolean shouldLoop = true;

    public String getElementId() {
        return elementId;
    }

    public int getTargetIndex() {
        return targetIndex;
    }

    public boolean shouldLoop() {
        return shouldLoop;
    }
}