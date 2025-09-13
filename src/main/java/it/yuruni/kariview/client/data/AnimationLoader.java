package it.yuruni.kariview.client.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import it.yuruni.kariview.Kariview;
import it.yuruni.kariview.client.data.actions.*;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

public class AnimationLoader {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON;

    static {
        RuntimeTypeAdapterFactory<Action> actionAdapterFactory = RuntimeTypeAdapterFactory.of(Action.class, "type")
                .registerSubtype(ShowElementAction.class, "show_element")
                .registerSubtype(HideElementAction.class, "hide_element")
                .registerSubtype(MoveElementAction.class, "move_element")
                .registerSubtype(PlaySoundAction.class, "play_sound")
                .registerSubtype(ShowVanillaHudAction.class, "show_vanilla_hud")
                .registerSubtype(HideVanillaHudAction.class, "hide_vanilla_hud");

        GSON = new GsonBuilder()
                .registerTypeAdapterFactory(actionAdapterFactory)
                .setPrettyPrinting()
                .create();
    }

    public static AnimationData loadAnimation(String namespace, String animationId) {
        File file = getAnimationFile(namespace, animationId);

        if (!file.exists()) {
            LOGGER.error("Animation file not found: " + file.getAbsolutePath());
            return null;
        }

        try (FileReader reader = new FileReader(file)) {
            return GSON.fromJson(reader, AnimationData.class);
        } catch (IOException e) {
            LOGGER.error("Failed to read animation file: " + file.getAbsolutePath(), e);
        } catch (JsonSyntaxException e) {
            LOGGER.error("Failed to parse animation JSON: " + file.getAbsolutePath(), e);
        }
        return null;
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
        // Create the template directory and file
        File templateDir = new File(mainDir, "example");
        if (templateDir.mkdirs()) {
            File templateFile = new File(templateDir, "template.json");
            try (FileWriter writer = new FileWriter(templateFile)) {
                String templateContent = "{\n" +
                        "  \"id\": \"template_animation\",\n" +
                        "  \"elements\": [\n" +
                        "    {\n" +
                        "      \"element_id\": \"example_image\",\n" +
                        "      \"type\": \"gui_element\",\n" +
                        "      \"texture_path\": \"example.png\"\n" +
                        "    }\n" +
                        "  ],\n" +
                        "  \"keyframes\": [\n" +
                        "    {\n" +
                        "      \"timestamp\": 0,\n" +
                        "      \"actions\": [\n" +
                        "        {\n" +
                        "          \"type\": \"show_element\",\n" +
                        "          \"element_id\": \"example_image\",\n" +
                        "          \"x\": 0,\n" +
                        "          \"y\": 0,\n" +
                        "          \"width\": 128,\n" +
                        "          \"height\": 128\n" +
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
        }
    }

    private static File getAnimationFile(String namespace, String animationId) {
        Path mainDir = FMLPaths.GAMEDIR.get().resolve("kariviewlib");
        Path animationDir = mainDir.resolve(namespace);
        return animationDir.resolve(animationId + ".json").toFile();
    }
}