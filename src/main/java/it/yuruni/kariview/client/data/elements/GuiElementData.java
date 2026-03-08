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

    public void setId(String id) { this.elementId = id; }
    public void setTexturePath(String path) { this.texturePath = path; }
    public void setTexturePathPattern(String pattern) { this.texturePathPattern = pattern; }
}