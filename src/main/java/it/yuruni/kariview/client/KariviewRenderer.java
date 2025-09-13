package it.yuruni.kariview.client;

import com.mojang.logging.LogUtils;
import it.yuruni.kariview.Kariview;
import it.yuruni.kariview.client.animation.AnimationManager;
import it.yuruni.kariview.client.data.elements.GuiElementData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod.EventBusSubscriber(modid = Kariview.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class KariviewRenderer {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static boolean isGuiActive = false;
    private static final int TEXTURE_SIZE = 256;

    @SubscribeEvent
    public static void onRenderGui(RenderGuiOverlayEvent.Post event) {
        //if (!isGuiActive) return;

        Minecraft mc = Minecraft.getInstance();
        ForgeGui gui = (ForgeGui) mc.gui;
        GuiGraphics guiGraphics = event.getGuiGraphics();

        // Update the animation manager every frame, ELAPSED TIME IS STILL CALCULATED USING MILLISECONDS
        AnimationManager.tick();

        // Get the active elements from the animation manager
        for (GuiElementData element : AnimationManager.getActiveElements()) {
            LOGGER.info("Active element: " + element.getId());
            renderElement(guiGraphics, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), element);
        }
    }

    private static void renderElement(GuiGraphics guiGraphics, int screenWidth, int screenHeight, GuiElementData element) {
        int x = (int) (screenWidth * element.getPosX());
        int y = (int) (screenHeight * element.getPosY());

        ResourceLocation textureResource = new ResourceLocation(Kariview.MODID, element.getTexture());
        guiGraphics.blit(textureResource, x, y, 0, 0, element.getWidth(), element.getHeight(), TEXTURE_SIZE, TEXTURE_SIZE);
    }
}