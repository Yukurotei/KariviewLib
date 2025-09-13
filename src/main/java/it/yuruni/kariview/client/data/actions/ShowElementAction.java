package it.yuruni.kariview.client.data.actions;

import com.google.gson.annotations.SerializedName;

public class ShowElementAction implements Action {
    @SerializedName("element_id")
    private String elementId;
    private int x;
    private int y;
    private int width;
    private int height;
    @SerializedName("texture_width")
    private int textureWidth;
    @SerializedName("texture_height")
    private int textureHeight;

    public String getElementId() {
        return elementId;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getTextureWidth() {
        return textureWidth;
    }

    public int getTextureHeight() {
        return textureHeight;
    }
}