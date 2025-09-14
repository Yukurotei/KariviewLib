package it.yuruni.kariview.client.animation;

import com.mojang.blaze3d.platform.NativeImage;
import it.yuruni.kariview.client.data.AnimationLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class AssetManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Map<String, ResourceLocation> loadedTextures = new HashMap<>();

    public static ResourceLocation loadTexture(String namespace, String path) {
        String key = namespace + ":" + path;
        if (loadedTextures.containsKey(key)) {
            return loadedTextures.get(key);
        }

        File file = new File("kariviewlib/" + namespace + "/assets/textures/" + path);
        if (!file.exists()) {
            LOGGER.error("Texture file not found: " + file.getAbsolutePath());
            return null;
        }

        try (InputStream stream = new FileInputStream(file)) {
            NativeImage image = NativeImage.read(stream);
            ResourceLocation location = new ResourceLocation(namespace, path);
            Minecraft.getInstance().getTextureManager().register(location, new DynamicTexture(image));
            loadedTextures.put(key, location);
            return location;
        } catch (IOException e) {
            LOGGER.error("Failed to load texture from file: " + file.getAbsolutePath(), e);
            return null;
        }
    }

    public static void loadAllTextures() {
        loadedTextures.clear();
        AnimationLoader.ANIMATION_CACHE.values().forEach(animationData -> {
            animationData.getElements().stream()
                    .filter(elementData -> elementData.getTexture() != null)
                    .forEach(elementData -> loadTexture(animationData.getNamespace(), elementData.getTexture()));

            animationData.getElements().stream()
                    .filter(elementData -> elementData.getTexturePathPattern() != null)
                    .forEach(elementData -> {
                        String texturePathPattern = elementData.getTexturePathPattern();
                        int lastSlashIndex = texturePathPattern.lastIndexOf('/');
                        String baseDir;
                        String pattern;
                        if (lastSlashIndex >= 0) {
                            baseDir = texturePathPattern.substring(0, lastSlashIndex);
                            pattern = texturePathPattern.substring(lastSlashIndex + 1);
                        } else {
                            baseDir = "";
                            pattern = texturePathPattern;
                        }

                        try {
                            Path texturesPath = AnimationLoader.getAssetFile(animationData.getNamespace(), baseDir).toPath();
                            if (Files.exists(texturesPath)) {
                                try (Stream<Path> walk = Files.walk(texturesPath)) {
                                    walk.filter(path -> path.getFileName().toString().matches(pattern.replace("*", ".*")))
                                            .forEach(path -> {
                                                String relativePath = texturesPath.relativize(path).toString().replace("\\", "/");
                                                String fullPath = baseDir.isEmpty() ? relativePath : baseDir + "/" + relativePath;
                                                loadTexture(animationData.getNamespace(), fullPath.replace("assets/textures/", ""));
                                            });
                                }
                            }
                        } catch (IOException e) {
                            LOGGER.error("Failed to load sprites for pattern: " + texturePathPattern, e);
                        }
                    });
        });
    }

    public static File loadSound(String namespace, String path) {
        File file = new File("kariviewlib/" + namespace + "/assets/sounds/" + path);
        if (!file.exists()) {
            LOGGER.error("Sound file not found: " + file.getAbsolutePath());
            return null;
        }
        return file;
    }
}