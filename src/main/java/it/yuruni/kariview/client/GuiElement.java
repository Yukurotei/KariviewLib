package it.yuruni.kariview.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;

public class GuiElement {
    private ResourceLocation texture;
    private double x;
    private double y;
    private double width;
    private double height;
    private final int textureWidth;
    private final int textureHeight;
    private float opacity = 1.0f;
    private double angle = 0.0;
    private double xScale = 1.0;
    private double yScale = 1.0;
    private String shaderId = "default";

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

    public void setAngle(double newAngle) {
        this.angle = newAngle;
    }

    public double getAngle() {
        return this.angle;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public void setXScale(double newScale) {
        this.xScale = newScale;
    }

    public double getXScale() {
        return this.xScale;
    }

    public void setYScale(double newScale) {
        this.yScale = newScale;
    }

    public double getYScale() {
        return this.yScale;
    }

    public void render(GuiGraphics guiGraphics) {
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();

        int initialX = (int) (this.x * screenWidth);
        int initialY = (int) (this.y * screenHeight);
        int initialWidth = (int) (this.width * screenWidth);
        int initialHeight = (int) (this.height * screenHeight);

        int scaledWidth = (int) (initialWidth * xScale);
        int scaledHeight = (int) (initialHeight * yScale);

        // Maintain aspect ratio
        float aspect = (float) textureWidth / (float) textureHeight;
        if (scaledHeight > scaledWidth / aspect) {
            scaledHeight = (int) (scaledWidth / aspect);
        } else {
            scaledWidth = (int) (scaledHeight * aspect);
        }

        scaledWidth = (int) (scaledWidth * xScale);
        scaledHeight = (int) (scaledHeight * yScale);

        // Center the scaled element
        int drawX = initialX - (scaledWidth - initialWidth) / 2;
        int drawY = initialY - (scaledHeight - initialHeight) / 2;

        guiGraphics.blit(this.texture, drawX, drawY, 0, 0, scaledWidth, scaledHeight, scaledWidth, scaledHeight);
    }


    public String getShaderId() {
        return shaderId;
    }

    public void setShaderId(String shaderId) {
        this.shaderId = shaderId;
    }
}