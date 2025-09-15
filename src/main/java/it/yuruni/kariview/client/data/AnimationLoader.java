package it.yuruni.kariview.client.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import it.yuruni.kariview.client.animation.AssetManager;
import it.yuruni.kariview.client.data.actions.*;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class AnimationLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String DATA_PACK_FOLDER = "kariviewlib";
    private static final Gson GSON;
    public static final Map<String, AnimationData> ANIMATION_CACHE = new HashMap<>();

    static {
        RuntimeTypeAdapterFactory<Action> actionAdapter = RuntimeTypeAdapterFactory.of(Action.class, "type")
                .registerSubtype(ShowElementAction.class, "show_element")
                .registerSubtype(HideElementAction.class, "hide_element")
                .registerSubtype(PlaySoundAction.class, "play_sound")
                .registerSubtype(ShowVanillaHudAction.class, "show_vanilla_hud")
                .registerSubtype(HideVanillaHudAction.class, "hide_vanilla_hud")
                .registerSubtype(StopAllSoundAction.class, "stop_all_sounds")
                .registerSubtype(UpdateSpriteAction.class, "update_sprite")
                .registerSubtype(SetSpriteIndexAction.class, "set_sprite_index")
                .registerSubtype(StepSpriteIndexAction.class, "step_sprite_index")
                .registerSubtype(StopSpriteAnimationAction.class, "stop_sprite_animation")
                .registerSubtype(ScaleAction.class, "scale_element")
                .registerSubtype(MoveAction.class, "move_element")
                .registerSubtype(RotateAction.class, "rotate_element");

        GSON = new GsonBuilder()
                .registerTypeAdapterFactory(actionAdapter)
                .setLenient()
                .create();
    }

    public static void loadAllAnimations() {
        ANIMATION_CACHE.clear();
        File mainDir = FMLPaths.GAMEDIR.get().resolve(DATA_PACK_FOLDER).toFile();
        if (!mainDir.exists()) {
            LOGGER.warn("KariviewLib main directory not found. No animations will be loaded.");
            return;
        }

        try (Stream<Path> walk = Files.walk(mainDir.toPath())) {
            walk.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".json"))
                    .forEach(path -> {
                        try {
                            String relativePath = mainDir.toPath().relativize(path).toString().replace(File.separator, "/");
                            String[] parts = relativePath.split("/");

                            if (parts.length == 3 && parts[1].equals("animations")) {
                                String namespace = parts[0];
                                String animationId = parts[2].replace(".json", "");
                                loadAnimation(namespace, animationId);
                                LOGGER.info("Loading animation {}:{}", namespace, animationId);
                            } else {
                                LOGGER.warn("Skipping invalid animation file path: " + relativePath);
                            }
                        } catch (Exception e) {
                            LOGGER.error("Failed to process animation file: " + path, e);
                        }
                    });
        } catch (IOException e) {
            LOGGER.error("Failed to walk the animation directory.", e);
        }

        AssetManager.loadAllTextures();
    }

    public static void ensureMainDirectoryExists() {
        File mainDir = FMLPaths.GAMEDIR.get().resolve("kariviewlib").toFile();
        if (!mainDir.exists()) {
            LOGGER.info("Creating KariviewLib main directory at: " + mainDir.getAbsolutePath());
            if (mainDir.mkdirs()) {
                createTemplateAnimation(mainDir);
            } else {
                LOGGER.error("Failed to create KariviewLib main directory.");
            }
        }
    }

    private static void createTemplateAnimation(File mainDir) {
        File exampleDir = new File(mainDir, "example");
        File assetsDir = new File(exampleDir, "assets");
        File texturesDir = new File(assetsDir, "textures");
        File animationsDir = new File(exampleDir, "animations");

        if (exampleDir.mkdirs() && assetsDir.mkdirs() && texturesDir.mkdirs() && animationsDir.mkdirs()) {
            File templateFile = new File(animationsDir, "template.json");
            try (FileWriter writer = new FileWriter(templateFile)) {
                String templateContent = "{\n" +
                        "  \"id\": \"template_animation\",\n" +
                        "  \"elements\": [\n" +
                        "    {\n" +
                        "      \"element_id\": \"example_image\",\n" +
                        "      \"type\": \"gui_element\",\n" +
                        "      \"texture_path\": \"textures/example.png\"\n" +
                        "    }\n" +
                        "  ],\n" +
                        "  \"keyframes\": [\n" +
                        "    {\n" +
                        "      \"timestamp\": 0,\n" +
                        "      \"actions\": [\n" +
                        "        {\n" +
                        "          \"type\": \"show_element\",\n" +
                        "          \"element_id\": \"example_image\"\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}";
                writer.write(templateContent);
                LOGGER.info("Created template animation file at: " + templateFile.getAbsolutePath());
            } catch (IOException e) {
                LOGGER.error("Failed to write template animation file.", e);
            }

            File templateImageFile = new File(texturesDir, "example.png");
            try {
                // You will need a default image to be copied here. For now, we'll just log
                // that a placeholder is needed.
                LOGGER.info("Please add a placeholder image 'example.png' to the textures folder.");
            } catch (Exception e) {
                LOGGER.error("Failed to create template image file.", e);
            }
        } else {
            LOGGER.error("Failed to create all necessary subdirectories.");
        }
    }

    public static AnimationData loadAnimation(String namespace, String animationId) {
        String cacheKey = namespace + ":" + animationId;
        if (ANIMATION_CACHE.containsKey(cacheKey)) {
            return ANIMATION_CACHE.get(cacheKey);
        }

        try {
            File animationFile = getAnimationFile(namespace, animationId);
            if (!animationFile.exists()) {
                LOGGER.error("Animation file not found: " + animationFile.getAbsolutePath());
                return null;
            }

            try (FileReader reader = new FileReader(animationFile)) {
                AnimationData data = GSON.fromJson(reader, AnimationData.class);
                data.namespace = namespace;
                ANIMATION_CACHE.put(cacheKey, data);
                return data;
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load animation: " + cacheKey, e);
            return null;
        }
    }

    private static File getAnimationFile(String namespace, String animationId) {
        String gameDir = System.getProperty("user.dir");
        File dataPackDir = new File(gameDir, DATA_PACK_FOLDER);
        File namespaceDir = new File(dataPackDir, namespace);
        File animationsDir = new File(namespaceDir, "animations");
        return new File(animationsDir, animationId + ".json");
    }

    public static File getAssetFile(String namespace, String assetPath) {
        String gameDir = System.getProperty("user.dir");
        File dataPackDir = new File(gameDir, DATA_PACK_FOLDER);
        File namespaceDir = new File(dataPackDir, namespace);
        return new File(namespaceDir, assetPath);
    }
}