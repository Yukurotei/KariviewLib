package it.yuruni.kariview.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class GuiElement {
    private ResourceLocation texture;
    private double x;
    private double y;
    private final double width;
    private final double height;
    private final int textureWidth;
    private final int textureHeight;
    private double scale = 1.0;
    private float opacity = 1.0f;

    public GuiElement(ResourceLocation texture, double x, double y, double width, double height, int textureWidth, int textureHeight) {
        this.texture = texture;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
    }

    public void setTexture(ResourceLocation newTexture) {
        this.texture = newTexture;
    }

    public void setScale(double newScale) {
        this.scale = newScale;
    }

    public double getScale() {
        return this.scale;
    }

    public void setOpacity(float opacity) {
        this.opacity = opacity;
    }

    public float getOpacity() {
        return opacity;
    }

    public void setX(double newX) {
        this.x = newX;
    }

    public double getX() {
        return this.x;
    }

    public void setY(double newY) {
        this.y = newY;
    }

    public double getY() {
        return this.y;
    }

    public void render(GuiGraphics guiGraphics) {
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();

        // Initial dimensions in pixels
        int initialX = (int) (this.x * screenWidth);
        int initialY = (int) (this.y * screenHeight);
        int initialWidth = (int) (this.width * screenWidth);
        int initialHeight = (int) (this.height * screenHeight);

        // Apply scale
        int scaledWidth = (int) (initialWidth * scale);
        int scaledHeight = (int) (initialHeight * scale);

        // Maintain aspect ratio
        float aspect = (float) textureWidth / (float) textureHeight;
        if (scaledHeight > scaledWidth / aspect) {
            scaledHeight = (int) (scaledWidth / aspect);
        } else {
            scaledWidth = (int) (scaledHeight * aspect);
        }

        // Center the scaled element
        int drawX = initialX - (scaledWidth - initialWidth) / 2;
        int drawY = initialY - (scaledHeight - initialHeight) / 2;

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.opacity);

        guiGraphics.blit(
                texture,
                drawX, drawY,
                scaledWidth, scaledHeight,
                0.0f, 0.0f,
                textureWidth, textureHeight,
                textureWidth, textureHeight
        );

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

}