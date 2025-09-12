package it.yuruni.kariview.client;

import com.mojang.logging.LogUtils;
import it.yuruni.kariview.Kariview;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod.EventBusSubscriber(modid = Kariview.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class KariviewRenderer {

    private static final Logger LOGGER = LogUtils.getLogger();
    public static boolean isGuiActive = false;

    private static final GuiElement element = new GuiElement(new ResourceLocation(Kariview.MODID, "textures/gui/example.png"), 0, 0, 512, 288, 512, 288);

    @SubscribeEvent
    public static void onRenderGui(RenderGuiOverlayEvent.Post event) {
        if (!isGuiActive) return;

        Minecraft mc = Minecraft.getInstance();
        GuiGraphics guiGraphics = event.getGuiGraphics();

        element.render(guiGraphics);
    }
}