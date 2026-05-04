package it.yuruni.kariview.client;

import com.mojang.logging.LogUtils;
import it.yuruni.kariview.Kariview;
import it.yuruni.kariview.client.animation.AnimationManager;
import it.yuruni.kariview.client.camera.CameraController;
import it.yuruni.kariview.client.shader.FullscreenShaderRenderer;
import it.yuruni.kariview.client.shader.ShaderManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.PlayerModel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod.EventBusSubscriber(modid = Kariview.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class KariviewRenderer {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static boolean isGuiActive = false, renderShaderOnTop = false;
    public static String activeFullscreenShader = null;

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!CameraController.isActive()) return;
        CameraController.tick();
        // Always drive pitch during transitions; during ACTIVE only when locked
        if (CameraController.isInTransition() || CameraController.isPitchLocked()) {
            event.setPitch(CameraController.getPitch());
        }
        // Targeted yaw: animate during transitions, hold if also locked
        if (CameraController.hasYawTarget()) {
            if (CameraController.isInTransition() || CameraController.isYawLocked()) {
                event.setYaw(CameraController.getYaw());
            }
        } else if (CameraController.isYawLocked() && !CameraController.isInTransition()) {
            // Lock-only (no target): hold captured yaw during ACTIVE
            event.setYaw(CameraController.getLockedYaw());
        }
        event.setRoll(0f);
    }

    private static boolean hidHeadThisFrame = false;

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        hidHeadThisFrame = false;
        if (event.getEntity() != Minecraft.getInstance().player) return;

        if (CameraController.isHidingLocalPlayer()) {
            event.setCanceled(true);
            return;
        }

        if (CameraController.isHidingHead()) {
            PlayerModel<?> model = event.getRenderer().getModel();
            model.head.visible = false;
            model.hat.visible  = false;
            hidHeadThisFrame = true;
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        if (!hidHeadThisFrame) return;
        if (event.getEntity() != Minecraft.getInstance().player) return;
        PlayerModel<?> model = event.getRenderer().getModel();
        model.head.visible = true;
        model.hat.visible  = true;
        hidHeadThisFrame = false;
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiOverlayEvent.Post event) {
        if (!isGuiActive) return;
        AnimationManager.tick();

        GuiGraphics guiGraphics = event.getGuiGraphics();

        for (GuiElement element : AnimationManager.getActiveElements().values()) {
            element.render(guiGraphics);
        }

        /*
        if (activeFullscreenShader != null) {
            int programId = ShaderManager.getShaderProgram(activeFullscreenShader);
            if (programId > 0) {
                FullscreenShaderRenderer.renderFullscreenShader(guiGraphics, programId, event.getPartialTick());
            }
        }
         */
    }
}