package it.yuruni.kariview.client.data.actions;

import com.google.gson.annotations.SerializedName;

public class HideElementAction implements Action {
    @SerializedName("element_id")
    private String elementId;

    public String getElementId() {
        return elementId;
    }
}
