package it.yuruni.kariview.client;

import com.mojang.logging.LogUtils;
import it.yuruni.kariview.Kariview;
import it.yuruni.kariview.client.animation.AnimationManager;
import it.yuruni.kariview.client.shader.FullscreenShaderRenderer;
import it.yuruni.kariview.client.shader.ShaderManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod.EventBusSubscriber(modid = Kariview.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class KariviewRenderer {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static boolean isGuiActive = false, renderShaderOnTop = false;
    public static String activeFullscreenShader = "default";

    @SubscribeEvent
    public static void onRenderGui(RenderGuiOverlayEvent.Post event) {
        if (!isGuiActive) return;
        AnimationManager.tick();

        GuiGraphics guiGraphics = event.getGuiGraphics();

        // Render GUI elements first
        for (GuiElement element : AnimationManager.getActiveElements().values()) {
            element.render(guiGraphics);
        }

        // Render fullscreen shader on top
        if (activeFullscreenShader != null) {
            int programId = ShaderManager.getShaderProgram(activeFullscreenShader);
            if (programId > 0) {
                FullscreenShaderRenderer.renderFullscreenShader(guiGraphics, programId, event.getPartialTick());
            }
        }
    }
}