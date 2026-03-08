package it.yuruni.kariview.client.data.actions;

import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import it.yuruni.kariview.client.GuiElement;
import it.yuruni.kariview.client.animation.states.AnimationContext;
import it.yuruni.kariview.client.animation.AssetManager;
import it.yuruni.kariview.client.animation.SpriteManager;
import it.yuruni.kariview.client.animation.SpriteState;
import it.yuruni.kariview.client.data.VariableManager;
import it.yuruni.kariview.client.data.elements.GuiElementData;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.util.List;

public class ShowElementAction implements Action {
    private static final Logger LOGGER = LogUtils.getLogger();

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
    @SerializedName("texture_variable")
    private String textureVariable;

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

    public void setElementId(String elementId) { this.elementId = elementId; }
    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
    public void setScale(double scale) { this.scale = scale; }
    public void setTextureWidth(int textureWidth) { this.textureWidth = textureWidth; }
    public void setTextureHeight(int textureHeight) { this.textureHeight = textureHeight; }
    public void setStartOpacity(Float startOpacity) { this.startOpacity = startOpacity; }

    @Override
    public void execute(AnimationContext ctx) {
        GuiElementData elementData = ctx.currentAnimation.getElementById(elementId);
        if (elementData != null) {
            ResourceLocation textureResource = null;
            if (textureVariable != null && !textureVariable.isEmpty()) {
                String texPath = VariableManager.get(ctx.currentAnimation.getNamespace(), textureVariable);
                if (texPath != null) {
                    textureResource = AssetManager.loadTexture(ctx.currentAnimation.getNamespace(), texPath);
                }
            }
            if (textureResource == null) {
                if (elementData.getTexture() != null) {
                    textureResource = AssetManager.loadTexture(ctx.currentAnimation.getNamespace(), elementData.getTexture());
                } else if (elementData.getTexturePathPattern() != null) {
                    List<ResourceLocation> sprites = SpriteManager.loadSprites(ctx.currentAnimation.getNamespace(), elementData.getTexturePathPattern());
                    if (!sprites.isEmpty()) {
                        ctx.spriteStates.put(elementData.getId(), new SpriteState(sprites));
                        textureResource = sprites.get(0);
                    } else {
                        LOGGER.error("Failed to load sprites for element: {}", elementData.getId());
                        return;
                    }
                } else {
                    LOGGER.error("Element has no texture or texture pattern: {}", elementData.getId());
                    return;
                }
            }

            if (textureResource != null) {
                GuiElement newElement = new GuiElement(textureResource, x, y, scale, scale, textureWidth, textureHeight);
                if (startOpacity != null) {
                    newElement.setOpacity(startOpacity);
                }
                ctx.activeElements.put(elementId, newElement);
            } else {
                LOGGER.error("Failed to load texture for element: {}", elementData.getId());
            }
        } else {
            LOGGER.warn("ShowElementAction for non-existent element: {}", elementId);
        }
    }
}
