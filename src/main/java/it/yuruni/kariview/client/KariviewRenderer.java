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

    @SubscribeEvent
    public static void onRenderGui(RenderGuiOverlayEvent.Post event) {
        if (!isGuiActive) return;
        AnimationManager.tick();

        GuiGraphics guiGraphics = event.getGuiGraphics();

        for (GuiElement element : AnimationManager.getActiveElements().values()) {
            element.render(guiGraphics);
        }
    }

    /*
    @SubscribeEvent
    public static void onRenderPre(RenderGuiOverlayEvent.Pre event) {
        if (isGuiActive && event.getOverlay().id().getNamespace().equals("minecraft")) {
            event.setCanceled(true);
        }
    }
     */
}
