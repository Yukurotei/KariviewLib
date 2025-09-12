package it.yuruni.kariview.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class GuiElement {

    private final ResourceLocation texture;
    private final int x, y, width, height, textureWidth, textureHeight;

    public GuiElement(ResourceLocation texture, int x, int y, int textureWidth, int textureHeight, int width, int height) {
        this.texture = texture;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
    }

    public void render(GuiGraphics guiGraphics) {
        guiGraphics.blit(texture, this.x, this.y, 0, 0, this.width, this.height, this.textureWidth, this.textureHeight);
    }
}

