package it.yuruni.kariview.client.shader;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL20;

public class FullscreenShaderRenderer {

    public static void renderFullscreenShader(GuiGraphics guiGraphics, int shaderProgramId, float partialTick) {
        if (shaderProgramId <= 0) return;

        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Get current matrices
        Matrix4f projectionMatrix = new Matrix4f(RenderSystem.getProjectionMatrix());
        Matrix4f modelViewMatrix = new Matrix4f(RenderSystem.getModelViewMatrix());

        // Bind our custom shader
        GL20.glUseProgram(shaderProgramId);

        // Set uniforms
        int modelViewLoc = GL20.glGetUniformLocation(shaderProgramId, "ModelViewMat");
        if (modelViewLoc != -1) {
            float[] matrixData = new float[16];
            modelViewMatrix.get(matrixData);
            GL20.glUniformMatrix4fv(modelViewLoc, false, matrixData);
        }

        int projLoc = GL20.glGetUniformLocation(shaderProgramId, "ProjMat");
        if (projLoc != -1) {
            float[] matrixData = new float[16];
            projectionMatrix.get(matrixData);
            GL20.glUniformMatrix4fv(projLoc, false, matrixData);
        }

        // Set time uniform
        int timeLoc = GL20.glGetUniformLocation(shaderProgramId, "Time");
        if (timeLoc != -1) {
            long gameTime = mc.level != null ? mc.level.getGameTime() : 0;
            float time = (gameTime + partialTick) / 20.0f;
            GL20.glUniform1f(timeLoc, time);
        }

        // Draw fullscreen quad
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.getBuilder();

        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        bufferBuilder.vertex(0, screenHeight, 0).color(255, 255, 255, 255).endVertex();
        bufferBuilder.vertex(screenWidth, screenHeight, 0).color(255, 255, 255, 255).endVertex();
        bufferBuilder.vertex(screenWidth, 0, 0).color(255, 255, 255, 255).endVertex();
        bufferBuilder.vertex(0, 0, 0).color(255, 255, 255, 255).endVertex();

        tesselator.end();

        // Unbind shader and restore Minecraft's default
        GL20.glUseProgram(0);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        RenderSystem.disableBlend();
    }
}