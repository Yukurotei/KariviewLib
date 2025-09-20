package it.yuruni.kariview.client.data.actions;

import com.google.gson.annotations.SerializedName;

public class ShowElementAction implements Action {
    @SerializedName("element_id")
    private String elementId;
    private double x;
    private double y;
    private double scale;
    @SerializedName("texture_width")
    private int textureWidth;
    @SerializedName("texture_height")
    private int textureHeight;
    @SerializedName("start_opacity")
    private Float startOpacity;
    @SerializedName("target_opacity")
    private Float targetOpacity;
    @SerializedName("fade_duration")
    private Long fadeDuration;
    @SerializedName("fade_easing_type")
    private String fadeEasingType;

    public String getElementId() {
        return elementId;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getWidth() {
        return scale;
    }

    public double getHeight() {
        return scale;
    }

    public int getTextureWidth() {
        return textureWidth;
    }

    public int getTextureHeight() {
        return textureHeight;
    }

    public Float getStartOpacity() {
        return startOpacity;
    }

    public Float getTargetOpacity() {
        return targetOpacity;
    }

    public Long getFadeDuration() {
        return fadeDuration;
    }

    public String getFadeEasingType() {
        return fadeEasingType;
    }
}