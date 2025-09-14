package it.yuruni.kariview.client;

import com.mojang.blaze3d.systems.RenderSystem;
import it.yuruni.kariview.Kariview;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class GuiElement {
    private ResourceLocation texture;
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final int textureWidth;
    private final int textureHeight;

    public GuiElement(ResourceLocation texture, int x, int y, int width, int height, int textureWidth, int textureHeight) {
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


    public void render(GuiGraphics guiGraphics) {
        RenderSystem.setShaderTexture(0, texture);
        guiGraphics.blit(texture, x, y, 0, 0, width, height, textureWidth, textureHeight);
    }
}
