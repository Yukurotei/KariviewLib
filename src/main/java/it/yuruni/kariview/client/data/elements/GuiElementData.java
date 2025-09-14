package it.yuruni.kariview.client.data.elements;

import com.google.gson.annotations.SerializedName;

public class GuiElementData {
    @SerializedName("element_id")
    private String elementId;

    @SerializedName("texture_path")
    private String texturePath;

    @SerializedName("texture_path_pattern")
    private String texturePathPattern;

    public String getId() {
        return elementId;
    }

    public String getTexture() {
        return texturePath;
    }

    public String getTexturePathPattern() {
        return texturePathPattern;
    }
}