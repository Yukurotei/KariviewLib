package it.yuruni.kariview.client;

import com.mojang.blaze3d.systems.RenderSystem;
import it.yuruni.kariview.Kariview;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class GuiElement {
    private ResourceLocation texture;
    private final double x;
    private final double y;
    private final double width;
    private final double height;
    private final int textureWidth;
    private final int textureHeight;

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

    public void render(GuiGraphics guiGraphics) {
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();

        int finalX = (int) (this.x * screenWidth);
        int finalY = (int) (this.y * screenHeight);

        RenderSystem.setShaderTexture(0, texture);
        guiGraphics.blit(texture, finalX, finalY, 0, 0, (int)this.width, (int)this.height, textureWidth, textureHeight);
    }
}