package it.yuruni.kariview.client.data.actions;

import com.google.gson.annotations.SerializedName;

public class StopSpriteAnimationAction implements Action {
    @SerializedName("element_id")
    private String elementId;

    public String getElementId() {
        return elementId;
    }
}