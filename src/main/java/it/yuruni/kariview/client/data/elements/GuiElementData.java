package it.yuruni.kariview.client.data.elements;

import com.google.gson.annotations.SerializedName;

public class GuiElementData {
    @SerializedName("element_id")
    private String id;
    @SerializedName("texture_path")
    private String texture;

    public String getId() {
        return id;
    }

    public String getTexture() {
        return texture;
    }
}
