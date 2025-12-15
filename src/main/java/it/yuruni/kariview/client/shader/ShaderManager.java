package it.yuruni.kariview.client.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import it.yuruni.kariview.Kariview;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.slf4j.Logger;
import net.minecraft.client.renderer.GameRenderer;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;

public class ShaderManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, Integer> SHADER_PROGRAM_CACHE = new HashMap<>();
    private static final Map<Integer, Map<String, Integer>> UNIFORM_LOCATION_CACHE = new HashMap<>();

    public static int getShaderProgram(String shaderId) {
        return SHADER_PROGRAM_CACHE.computeIfAbsent(shaderId, ShaderManager::loadAndCompileShader);
    }

    private static String loadShaderSource(ResourceLocation location) {
        try {
            Optional<Resource> optionalResource = Minecraft.getInstance()
                    .getResourceManager()
                    .getResource(location);

            if (optionalResource.isEmpty()) {
                LOGGER.error("Shader resource not found: {}", location);
                return null;
            }

            try (InputStream inputStream = optionalResource.get().open()) {
                return new Scanner(inputStream, StandardCharsets.UTF_8).useDelimiter("\\A").next();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load shader source: {}", location, e);
            return null;
        }
    }


    private static int compileShader(String source, int type) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);

        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            LOGGER.error("Shader compilation failed: {}", GL20.glGetShaderInfoLog(shader, 1024));
            GL20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    private static int loadAndCompileShader(String shaderId) {
        LOGGER.info("Loading shader {}", shaderId);
        ResourceLocation vshLoc = new ResourceLocation(Kariview.MODID, "shaders/" + shaderId + ".vsh");
        ResourceLocation fshLoc = new ResourceLocation(Kariview.MODID, "shaders/" + shaderId + ".fsh");

        String vshSource = loadShaderSource(vshLoc);
        String fshSource = loadShaderSource(fshLoc);

        if (vshSource == null || fshSource == null) return 0;

        int vsh = compileShader(vshSource, GL20.GL_VERTEX_SHADER);
        int fsh = compileShader(fshSource, GL20.GL_FRAGMENT_SHADER);

        if (vsh == 0 || fsh == 0) return 0;

        int program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vsh);
        GL20.glAttachShader(program, fsh);
        GL20.glLinkProgram(program);

        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            LOGGER.error("Shader program linking failed: {}", GL20.glGetProgramInfoLog(program, 1024));
            GL20.glDeleteProgram(program);
            return 0;
        }

        GL20.glDetachShader(program, vsh);
        GL20.glDetachShader(program, fsh);
        GL20.glDeleteShader(vsh);
        GL20.glDeleteShader(fsh);

        LOGGER.info("Successfully compiled and linked shader program: {}", shaderId);

        // Cache uniform locations
        GL20.glUseProgram(program);
        Map<String, Integer> uniforms = new HashMap<>();
        uniforms.put("Time", GL20.glGetUniformLocation(program, "Time"));
        uniforms.put("AspectRatio", GL20.glGetUniformLocation(program, "AspectRatio"));
        uniforms.put("Sampler", GL20.glGetUniformLocation(program, "Sampler0"));
        LOGGER.info("Uniform locations - ModelViewMat: {}, ProjMat: {}, Sampler0: {}",
                GL20.glGetUniformLocation(program, "ModelViewMat"),
                GL20.glGetUniformLocation(program, "ProjMat"),
                GL20.glGetUniformLocation(program, "Sampler0"));

        UNIFORM_LOCATION_CACHE.put(program, uniforms);

        // Set the sampler to texture unit 0. This only needs to be done once.
        int samplerLoc = uniforms.get("Sampler");
        if (samplerLoc != -1) {
            GL20.glUniform1i(samplerLoc, 0);
        }

        // Unbind the program for now.
        GL20.glUseProgram(0);

        return program;
    }

    /**
     * Binds a shader program by its ID and sets common uniforms.
     */
    public static void useShader(int programId, float partialTicks, PoseStack poseStack) {
        if (programId > 0) {
            RenderSystem.assertOnRenderThread();
            GL20.glUseProgram(programId);

            // Get the current matrices from the pose stack
            Matrix4f modelViewMatrix = new Matrix4f(RenderSystem.getModelViewMatrix());
            modelViewMatrix.mul(poseStack.last().pose());

            // Set ModelViewMat uniform
            int modelViewLoc = GL20.glGetUniformLocation(programId, "ModelViewMat");
            if (modelViewLoc != -1) {
                float[] matrixData = new float[16];
                modelViewMatrix.get(matrixData);
                GL20.glUniformMatrix4fv(modelViewLoc, false, matrixData);
            }

            // Set ProjMat uniform
            int projLoc = GL20.glGetUniformLocation(programId, "ProjMat");
            if (projLoc != -1) {
                float[] matrixData = new float[16];
                RenderSystem.getProjectionMatrix().get(matrixData);
                GL20.glUniformMatrix4fv(projLoc, false, matrixData);
            }

            // Set sampler uniform
            int samplerLoc = GL20.glGetUniformLocation(programId, "Sampler0");
            if (samplerLoc != -1) {
                GL20.glUniform1i(samplerLoc, 0);
            }

            if (UNIFORM_LOCATION_CACHE.containsKey(programId)) {
                Map<String, Integer> uniforms = UNIFORM_LOCATION_CACHE.get(programId);

                // Update Time uniform
                int timeLoc = uniforms.getOrDefault("Time", -1);
                if (timeLoc != -1) {
                    long gameTime = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0;
                    float time = (gameTime + partialTicks) / 20.0f;
                    GL20.glUniform1f(timeLoc, time);
                }

                // Update AspectRatio uniform
                int aspectLoc = uniforms.getOrDefault("AspectRatio", -1);
                if (aspectLoc != -1) {
                    var window = Minecraft.getInstance().getWindow();
                    float aspectRatio = (float) window.getGuiScaledWidth() / (float) window.getGuiScaledHeight();
                    GL20.glUniform1f(aspectLoc, aspectRatio);
                }
            }
        }
    }

    public static void useShaderWithMatrices(int programId, float partialTicks, Matrix4f modelViewMatrix, Matrix4f projectionMatrix) {
        if (programId > 0) {
            RenderSystem.assertOnRenderThread();
            GL20.glUseProgram(programId);

            // Set ModelViewMat uniform
            int modelViewLoc = GL20.glGetUniformLocation(programId, "ModelViewMat");
            if (modelViewLoc != -1) {
                float[] matrixData = new float[16];
                modelViewMatrix.get(matrixData);
                GL20.glUniformMatrix4fv(modelViewLoc, false, matrixData);
            }

            // Set ProjMat uniform
            int projLoc = GL20.glGetUniformLocation(programId, "ProjMat");
            if (projLoc != -1) {
                float[] matrixData = new float[16];
                projectionMatrix.get(matrixData);
                GL20.glUniformMatrix4fv(projLoc, false, matrixData);
            }

            // Set sampler uniform
            int samplerLoc = GL20.glGetUniformLocation(programId, "Sampler0");
            if (samplerLoc != -1) {
                GL20.glUniform1i(samplerLoc, 0);
            }

            if (UNIFORM_LOCATION_CACHE.containsKey(programId)) {
                Map<String, Integer> uniforms = UNIFORM_LOCATION_CACHE.get(programId);

                // Update Time uniform
                int timeLoc = uniforms.getOrDefault("Time", -1);
                if (timeLoc != -1) {
                    long gameTime = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0;
                    float time = (gameTime + partialTicks) / 20.0f;
                    GL20.glUniform1f(timeLoc, time);
                }

                // Update AspectRatio uniform
                int aspectLoc = uniforms.getOrDefault("AspectRatio", -1);
                if (aspectLoc != -1) {
                    var window = Minecraft.getInstance().getWindow();
                    float aspectRatio = (float) window.getGuiScaledWidth() / (float) window.getGuiScaledHeight();
                    GL20.glUniform1f(aspectLoc, aspectRatio);
                }
            }
        }
    }

    /**
     * Stops using the current shader program.
     */
    public static void stopShader() {
        // Unbind our custom shader program
        GL20.glUseProgram(0);

        // Restore to a default shader to prevent state pollution for subsequent rendering
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
    }
}